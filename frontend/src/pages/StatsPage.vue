<template>
  <q-page class="page">
    <div class="row items-end justify-between q-mb-lg" style="gap: 16px">
      <div>
        <h1 class="page-title">統計</h1>
        <p class="text-sub q-ma-none" style="font-size: 15px">{{ rangeLabel }} · {{ scopeLabel }} · 看看這段時間把時間花在哪裡</p>
      </div>
      <div class="row items-center" style="gap: 10px">
        <q-select
          v-model="scope" :options="scopeOptions" emit-value map-options dense outlined
          aria-label="統計範圍" style="min-width: 180px"
        >
          <template #option="{ itemProps, opt }">
            <q-item v-bind="itemProps">
              <q-item-section avatar><span class="cat-dot" :style="{ background: opt.color }" /></q-item-section>
              <q-item-section>{{ opt.label }}</q-item-section>
            </q-item>
          </template>
          <template #selected-item="{ opt }">
            <span class="row items-center no-wrap" style="gap: 8px"><span class="cat-dot" :style="{ background: opt.color }" />{{ opt.label }}</span>
          </template>
        </q-select>
        <q-btn-toggle
          v-model="period"
          class="fun-toggle"
          no-caps unelevated
          toggle-color="primary" toggle-text-color="dark"
          :options="PERIODS"
          aria-label="統計期間"
        />
        <template v-if="period === 'custom'">
          <q-input v-model="custom.from" type="date" dense outlined label="從" stack-label :max="custom.to" style="width: 160px" />
          <q-input v-model="custom.to" type="date" dense outlined label="到" stack-label :min="custom.from" style="width: 160px" />
        </template>
      </div>
    </div>

    <div class="kpis q-mb-lg">
      <div class="kpi bg-accent-fun">
        <span class="kpi-label">工時總計</span>
        <span class="kpi-value font-round">{{ hoursNum(total) }}<small>h</small></span>
        <span class="kpi-note">{{ worked ? `有排工時的 ${worked} 天，平均每天 ${formatHours(total / worked)}` : '這段期間還沒排工時' }}</span>
      </div>
      <div class="kpi bg-surface">
        <span class="kpi-label">未完成</span>
        <span class="kpi-value font-round">{{ counts.open }}<small>個</small></span>
        <span class="kpi-note">目前還在進行中</span>
      </div>
      <div class="kpi bg-mint">
        <span class="kpi-label">已完成</span>
        <span class="kpi-value font-round">{{ counts.done }}<small>個</small></span>
        <span class="kpi-note">全部時間累計</span>
      </div>
      <div class="kpi" :class="counts.overdue ? 'bg-danger' : 'bg-surface'">
        <span class="kpi-label">逾期</span>
        <span class="kpi-value font-round">{{ counts.overdue }}<small>個</small></span>
        <span class="kpi-note">未完成且過了死線</span>
      </div>
      <div class="kpi bg-butter">
        <span class="kpi-label">本期完成</span>
        <span class="kpi-value font-round">{{ counts.doneInPeriod }}<small>個</small></span>
        <span class="kpi-note">這段期間勾選完成</span>
      </div>
    </div>

    <div class="charts">
      <section class="fun-card chart-card" style="flex: 3 1 420px" aria-label="工時長條圖">
        <div class="row justify-between items-baseline">
          <h2 class="section-title">{{ bars.perWeek ? '每週工時' : '每天工時' }}</h2>
          <span class="text-sub" style="font-size: 13px">單位：小時</span>
        </div>
        <div class="bar-chart">
          <div class="y-axis font-round text-sub">
            <span v-for="t in ticks" :key="t" :style="{ bottom: `${t / scaleTop * 100}%` }">{{ t / 60 }}</span>
          </div>
          <div class="plot" role="img" :aria-label="barsAria">
            <div v-for="t in ticks" :key="t" class="gridline" :style="{ bottom: `${t / scaleTop * 100}%` }" />
            <div class="bars" :style="{ gap: bars.list.length > 14 ? '4px' : '14px' }">
              <div v-for="(b, i) in bars.list" :key="i" class="bar-col">
                <span v-if="showValues && b.minutes" class="bar-value font-round">{{ hoursNum(b.minutes) }}</span>
                <div class="bar" :class="{ current: isCurrent(b), empty: !b.minutes }" :style="{ height: `${b.minutes / scaleTop * 100}%` }" />
                <q-tooltip>{{ b.title }}　{{ formatHours(b.minutes) }}</q-tooltip>
              </div>
            </div>
          </div>
        </div>
        <div class="bars x-labels" :style="{ gap: bars.list.length > 14 ? '4px' : '14px' }">
          <span v-for="(b, i) in bars.list" :key="i" :class="{ current: isCurrent(b) }">{{ showLabel(i) ? b.label : '' }}</span>
        </div>
      </section>

      <section class="fun-card chart-card" style="flex: 2 1 320px" :aria-label="pieTitle">
        <h2 class="section-title">{{ pieTitle }}</h2>
        <div v-if="!cats.length" class="text-sub q-py-xl text-center">這段期間還沒有排工時，到時程表排一些吧！</div>
        <div v-else class="row items-center" style="gap: 24px">
          <div class="donut" role="img" :aria-label="catsAria" :style="{ background: donutGradient(cats, 'var(--surface)') }">
            <div class="donut-hole">
              <span class="font-round text-weight-bold" style="font-size: 28px; line-height: 1">{{ formatHours(total) }}</span>
              <span class="text-sub" style="font-size: 12px">總計</span>
            </div>
          </div>
          <ul class="legend">
            <li v-for="c in cats" :key="c.id ?? 'none'">
              <span class="swatch" :style="{ background: c.color }" />
              <span class="col">{{ c.name }}</span>
              <span class="font-round text-weight-bold">{{ formatHours(c.minutes) }}</span>
              <span class="font-round text-sub pct">{{ pct(c.minutes) }}</span>
            </li>
          </ul>
        </div>
      </section>
    </div>

    <!-- 單一專案（或無專案）：各任務工時排行 -->
    <section v-if="scope !== 'all'" class="fun-card chart-card q-mt-lg" aria-label="各任務工時">
      <div class="row justify-between items-baseline">
        <h2 class="section-title">各任務工時</h2>
        <span class="text-sub" style="font-size: 13px">只列這段期間有排工時的任務</span>
      </div>
      <div v-if="!ranking.length" class="text-sub q-py-lg text-center">這段期間沒有任務排工時</div>
      <div v-else class="table-box">
        <table>
          <thead>
            <tr>
              <th scope="col">任務</th>
              <th scope="col">分類</th>
              <th scope="col" class="text-right">工時</th>
              <th scope="col" class="text-right">佔比</th>
              <th scope="col">狀態</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in ranking" :key="r.id">
              <td class="text-weight-bold">{{ r.title }}</td>
              <td><span class="cat-chip" :style="{ background: categories.label(r.category_id).color }">{{ categories.label(r.category_id).name }}</span></td>
              <td class="text-right font-round text-weight-bold">{{ formatHours(r.minutes) }}</td>
              <td class="text-right">
                <div class="row items-center justify-end no-wrap" style="gap: 8px">
                  <div class="mini-bar" aria-hidden="true"><div :style="{ width: pct(r.minutes) }" /></div>
                  <span class="font-round text-sub" style="min-width: 36px">{{ pct(r.minutes) }}</span>
                </div>
              </td>
              <td><span class="state" :class="{ done: r.done }">{{ r.done ? '已完成' : '進行中' }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </q-page>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useTasksStore } from '../stores/tasks'
import { useCategoriesStore } from '../stores/categories'
import { useProjectsStore } from '../stores/projects'
import { useBlocksStore } from '../stores/blocks'
import { formatHours, sameDay, weekStart } from '../utils/timeline'
import { buckets, byCategory, byProject, byTask, donutGradient, inRange, periodRange, taskCounts, toDateInput, totalMinutes, workedDays } from '../utils/stats'
import { matchProject, NO_PROJECT_COLOR } from '../utils/task'

const tasks = useTasksStore()
const categories = useCategoriesStore()
const projects = useProjectsStore()
const blocks = useBlocksStore()

const PERIODS = [
  { label: '本週', value: 'week' },
  { label: '本月', value: 'month' },
  { label: '自訂日期', value: 'custom' }
]
const period = ref('week')
const custom = ref({ from: toDateInput(weekStart(new Date())), to: toDateInput(new Date()) })

// 自訂日期填反了就自動對調
const range = computed(() => {
  const c = custom.value
  const swapped = c.from && c.to && c.from > c.to ? { from: c.to, to: c.from } : c
  return periodRange(period.value, new Date(), swapped)
})
const md = d => `${d.getMonth() + 1}/${String(d.getDate()).padStart(2, '0')}`
const rangeLabel = computed(() => `${md(range.value.from)} – ${md(new Date(range.value.to - 1))}`)

/* 統計範圍：全部專案 / 單一專案（含已封存）/ 無專案；值為 'all' | 'none' | 專案 id */
const scope = ref('all')
const scopeOptions = computed(() => [
  { label: '全部專案', value: 'all', color: 'transparent' },
  ...projects.list.map(p => ({ label: p.archived ? `${p.name}（已封存）` : p.name, value: p.id, color: p.color })),
  { label: '無專案', value: 'none', color: NO_PROJECT_COLOR }
])
const scopeLabel = computed(() => scopeOptions.value.find(o => o.value === scope.value)?.label || '全部專案')
const scopeTasks = computed(() => tasks.tasks.filter(t => matchProject(t, scope.value)))

const periodBlocks = computed(() => {
  const inScope = new Set(scopeTasks.value.map(t => t.id))
  return inRange(blocks.list, range.value.from, range.value.to).filter(b => scope.value === 'all' || inScope.has(b.task_id))
})
const total = computed(() => totalMinutes(periodBlocks.value))
const worked = computed(() => workedDays(periodBlocks.value)) // 平均只除以有排工時的天數
const counts = computed(() => taskCounts(scopeTasks.value, range.value.from, range.value.to))
// 全部專案：各專案佔比；單一專案：專案內各分類佔比
const pieTitle = computed(() => scope.value === 'all' ? '各專案工時' : '各分類工時')
const cats = computed(() => scope.value === 'all'
  ? byProject(periodBlocks.value, tasks.tasks, projects.list)
  : byCategory(periodBlocks.value, tasks.tasks, categories.list.filter(c => c.project_id === scope.value)))
const ranking = computed(() => byTask(periodBlocks.value, scopeTasks.value))

/* 長條圖 */
const bars = computed(() => buckets(periodBlocks.value, range.value.from, range.value.to))
// 刻度上限取整數小時，至少 1 小時
const scaleTop = computed(() => Math.max(60, Math.ceil(Math.max(...bars.value.list.map(b => b.minutes), 0) / 60) * 60))
const ticks = computed(() => {
  const top = scaleTop.value
  return top >= 120 && (top / 60) % 2 === 0 ? [0, top / 2, top] : [0, top]
})
// 長條太多時只在 hover 看數字，x 軸標籤也隔幾根才標
const showValues = computed(() => bars.value.list.length <= 14)
const showLabel = i => bars.value.list.length <= 14 || i % 5 === 0
const isCurrent = b => !bars.value.perWeek && sameDay(b.from, new Date())

const hoursNum = m => Math.round(m / 60 * 10) / 10
const pct = m => total.value ? `${Math.round(m / total.value * 100)}%` : ''
const barsAria = computed(() => bars.value.list.map(b => `${b.title} ${formatHours(b.minutes)}`).join('、'))
const catsAria = computed(() => cats.value.map(c => `${c.name} ${formatHours(c.minutes)}`).join('、'))

onMounted(() => Promise.all([tasks.load(), categories.load(), projects.load(), blocks.load()]))
</script>

<style scoped>
.page { padding: 32px 40px 48px; }
@media (max-width: 600px) { .page { padding: 20px 16px 32px; } }
.section-title { margin: 0; font-size: 20px; font-weight: 700; line-height: 1.4; }

.kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 18px; }
.kpi {
  display: flex; flex-direction: column; gap: 8px; padding: 16px 18px;
  border-radius: 20px; border: 2px solid var(--line); box-shadow: 4px 4px 0 var(--edge);
}
.kpi-label { font-size: 14px; font-weight: 700; }
.kpi-value { font-size: 44px; font-weight: 700; line-height: 1; }
.kpi-value small { font-size: 18px; margin-left: 4px; }
.kpi-note { font-size: 12px; opacity: .85; }

.charts { display: flex; flex-wrap: wrap; gap: 20px; align-items: stretch; }
.chart-card { min-width: 0; display: flex; flex-direction: column; gap: 16px; padding: 20px 22px; }

.bar-chart { display: flex; gap: 8px; height: 240px; padding-top: 10px; }
.y-axis { position: relative; width: 24px; flex-shrink: 0; font-size: 12px; }
.y-axis span { position: absolute; right: 0; transform: translateY(50%); }
.plot { position: relative; flex: 1; min-width: 0; border-bottom: 2px solid var(--line); }
.gridline { position: absolute; left: 0; right: 0; border-top: 2px dashed var(--grid); }
.bars { display: grid; grid-auto-flow: column; grid-auto-columns: minmax(0, 1fr); height: 100%; position: relative; padding: 0 4px; }
.bar-col { display: flex; flex-direction: column; justify-content: flex-end; align-items: center; gap: 4px; height: 100%; cursor: default; }
.bar {
  width: 100%; max-width: 56px; box-sizing: border-box;
  border: 2px solid var(--line); border-bottom: none; border-radius: 10px 10px 0 0;
  background: #FFC49E;
}
.bar.current { background: var(--accent); }
.bar.empty { border: none; }
.bar-value { font-size: 13px; font-weight: 600; }
.x-labels { margin: -8px 0 0 32px; height: auto; font-size: 13px; color: var(--sub); text-align: center; }
.x-labels .current { color: var(--ink); font-weight: 700; }

.donut {
  position: relative; width: 190px; height: 190px; flex-shrink: 0; border-radius: 50%;
  border: 2px solid var(--line); box-shadow: 4px 4px 0 var(--edge);
}
.donut-hole {
  position: absolute; inset: 44px; border-radius: 50%; background: var(--surface); border: 2px solid var(--line);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.legend { flex: 1 1 160px; margin: 0; padding: 0; list-style: none; display: flex; flex-direction: column; gap: 10px; }
.legend li { display: flex; align-items: center; gap: 10px; font-size: 15px; }
.swatch { width: 16px; height: 16px; flex-shrink: 0; border-radius: 6px; border: 2px solid #2A2633; }
.pct { min-width: 40px; text-align: right; font-size: 13px; }
.table-box { overflow-x: auto; }
table { width: 100%; min-width: 620px; border-collapse: collapse; font-size: 15px; }
th { text-align: left; padding: 12px; color: var(--sub); font-size: 13px; }
td { padding: 10px 12px; border-top: 2px dashed var(--grid); }
.mini-bar { width: 80px; height: 10px; border-radius: 999px; border: 2px solid var(--line); background: var(--soft); overflow: hidden; }
.mini-bar > div { height: 100%; background: var(--accent); }
.state { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 13px; background: var(--soft); white-space: nowrap; }
.state.done { background: var(--mint); color: var(--on-color); }
</style>
