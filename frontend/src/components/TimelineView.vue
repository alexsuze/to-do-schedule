<template>
  <div>
    <div class="row items-center q-gutter-sm q-mb-sm">
      <q-btn-toggle v-model="scale" unelevated toggle-color="primary" size="sm"
        :options="[{ label: '日', value: 'day' }, { label: '週', value: 'week' }, { label: '月', value: 'month' }]" />
      <q-btn flat dense icon="today" label="回到今天" @click="scrollToToday" />
      <span class="text-caption text-grey">唯讀，點擊長條可編輯</span>
    </div>

    <!-- ponytail: 純 CSS 甘特圖，每天固定像素寬；資料量大再考慮虛擬捲動 -->
    <div ref="scrollEl" class="tl-scroll">
      <div :style="{ width: LABEL_W + totalW + 'px' }">
        <div class="tl-row tl-header">
          <div class="tl-label">事項</div>
          <div class="tl-track" :style="trackStyle">
            <div v-for="s in segments" :key="s.key" class="tl-seg" :style="{ width: s.days * ppd + 'px' }">{{ s.label }}</div>
          </div>
        </div>

        <div class="tl-body">
          <div class="tl-today" :style="{ left: LABEL_W + todayX + 'px' }" />
          <div v-for="t in scheduled" :key="t.id" class="tl-row">
            <div class="tl-label ellipsis cursor-pointer" @click="$emit('edit', t)">{{ t.title }}</div>
            <div class="tl-track" :style="trackStyle">
              <div class="tl-bar cursor-pointer" :class="barClass(t)" :style="barStyle(t)" @click="$emit('edit', t)">
                <span class="ellipsis">{{ t.title }}</span>
              </div>
            </div>
          </div>
          <div v-if="!scheduled.length" class="tl-row"><div class="tl-label text-grey">沒有已排程事項</div></div>
        </div>
      </div>
    </div>

    <div v-if="unscheduled.length" class="q-mt-md">
      <div class="text-subtitle1 q-mb-xs">未排程 <q-badge color="grey-5">{{ unscheduled.length }}</q-badge></div>
      <q-list bordered separator dense>
        <q-item v-for="t in unscheduled" :key="t.id" clickable @click="$emit('edit', t)">
          <q-item-section avatar><q-badge :color="priorityColor(t.priority)" rounded /></q-item-section>
          <q-item-section :class="{ 'text-strike text-grey': t.completed_at }">{{ t.title }}</q-item-section>
        </q-item>
      </q-list>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { priorityColor } from '../utils/task'

const props = defineProps({
  tasks: { type: Array, required: true } // 已套用篩選
})
defineEmits(['edit'])

const LABEL_W = 200
const PPD = { day: 48, week: 20, month: 6 } // 每天像素寬
const scale = ref('week')
const scrollEl = ref(null)
const ppd = computed(() => PPD[scale.value])

const DAY = 86400000
const startOfDay = d => { const x = new Date(d); x.setHours(0, 0, 0, 0); return x }
const addDays = (d, n) => new Date(d.getTime() + n * DAY)
const diffDays = (a, b) => Math.round((startOfDay(b) - startOfDay(a)) / DAY)
const today = startOfDay(new Date())

const scheduled = computed(() => props.tasks.filter(t => t.start_at || t.due_at))
const unscheduled = computed(() => props.tasks.filter(t => !t.start_at && !t.due_at))

// 事項起訖（沒開始就用到期當天，沒到期就用開始當天）
const span = t => {
  const s = startOfDay(new Date(t.start_at || t.due_at))
  const e = startOfDay(new Date(t.due_at || t.start_at))
  return { s, e: e < s ? s : e }
}

// 範圍：涵蓋所有事項與今天前後，前後各留 3 天
const range = computed(() => {
  let min = addDays(today, -7)
  let max = addDays(today, 30)
  for (const t of scheduled.value) {
    const { s, e } = span(t)
    if (s < min) min = s
    if (e > max) max = e
  }
  min = addDays(min, -3)
  max = addDays(max, 3)
  return { start: min, days: diffDays(min, max) + 1 }
})

const totalW = computed(() => range.value.days * ppd.value)
const todayX = computed(() => diffDays(range.value.start, today) * ppd.value)
const trackStyle = computed(() => ({
  width: totalW.value + 'px',
  backgroundSize: ppd.value + 'px 100%'
}))

// 表頭分段：日 / 週（週一起）/ 月
const WEEKDAY = ['日', '一', '二', '三', '四', '五', '六']
const segments = computed(() => {
  const out = []
  for (let i = 0; i < range.value.days; i++) {
    const d = addDays(range.value.start, i)
    let key, label
    if (scale.value === 'day') { key = i; label = `${d.getMonth() + 1}/${d.getDate()} ${WEEKDAY[d.getDay()]}` }
    else if (scale.value === 'week') { key = diffDays(new Date(2024, 0, 1), d) - ((d.getDay() + 6) % 7); label = `${d.getMonth() + 1}/${d.getDate()}` }
    else { key = `${d.getFullYear()}-${d.getMonth()}`; label = `${d.getFullYear()}/${d.getMonth() + 1}` }
    const last = out[out.length - 1]
    if (last && last.key === key) last.days++
    else out.push({ key, label, days: 1 })
  }
  return out
})

function barStyle (t) {
  const { s, e } = span(t)
  return {
    left: diffDays(range.value.start, s) * ppd.value + 'px',
    width: (diffDays(s, e) + 1) * ppd.value - 2 + 'px'
  }
}

function barClass (t) {
  return t.completed_at ? 'bg-grey-5' : `bg-${priorityColor(t.priority)}`
}

function scrollToToday () {
  if (scrollEl.value) scrollEl.value.scrollLeft = Math.max(0, todayX.value - 150)
}

onMounted(scrollToToday)
watch(scale, () => nextTick(scrollToToday))
</script>

<style scoped>
.tl-scroll { overflow-x: auto; border: 1px solid #e0e0e0; border-radius: 4px; background: #fff; }
.tl-row { display: flex; align-items: stretch; border-bottom: 1px solid #eee; min-height: 32px; }
.tl-header { position: sticky; top: 0; background: #fafafa; z-index: 2; font-size: 12px; }
.tl-label {
  width: 200px; min-width: 200px; padding: 6px 8px; box-sizing: border-box;
  position: sticky; left: 0; background: #fff; z-index: 3; border-right: 1px solid #e0e0e0;
  display: flex; align-items: center;
}
.tl-header .tl-label { background: #fafafa; }
.tl-track {
  position: relative; display: flex;
  background-image: linear-gradient(to right, #eee 1px, transparent 1px);
}
.tl-seg { border-right: 1px solid #ddd; padding: 6px 4px; box-sizing: border-box; white-space: nowrap; overflow: hidden; text-align: center; }
.tl-body { position: relative; }
.tl-today { position: absolute; top: 0; bottom: 0; width: 2px; background: #c10015; z-index: 1; }
.tl-bar {
  position: absolute; top: 5px; height: 22px; border-radius: 4px; color: #fff; font-size: 12px;
  padding: 0 6px; line-height: 22px; overflow: hidden; white-space: nowrap; z-index: 1;
}
</style>
