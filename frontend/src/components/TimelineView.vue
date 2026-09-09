<template>
  <div>
    <div class="row items-center q-gutter-sm q-mb-sm">
      <q-btn flat dense icon="today" label="回到今天" @click="scrollToToday" />
      <span class="text-caption text-grey">唯讀，點擊長條可編輯；捲到兩端可顯示更多日期</span>
    </div>

    <!-- ponytail: 純 CSS 甘特圖，每天固定像素寬；預設今天前 5 天、後 25 天，兩端可各加 10 天 -->
    <div class="tl-wrap">
      <div ref="scrollEl" class="tl-scroll" @scroll.passive="onScroll">
        <div :style="{ width: LABEL_W + totalW + 'px' }">
          <div class="tl-row tl-header">
            <div class="tl-label">事項</div>
            <div class="tl-track" :style="trackStyle">
              <div v-for="d in days" :key="d.key" class="tl-seg" :class="{ 'tl-weekend': d.weekend, 'tl-is-today': d.today }"
                :style="{ width: PPD + 'px' }">
                <div>{{ d.md }}</div>
                <div class="text-grey">{{ d.wd }}</div>
              </div>
            </div>
          </div>

          <div class="tl-body">
            <div class="tl-today" :style="{ left: LABEL_W + todayX + 'px' }" />
            <div v-for="t in scheduled" :key="t.id" class="tl-row">
              <div class="tl-label cursor-pointer" :style="labelStyle(t)" @click="$emit('edit', t)">
                <span class="ellipsis">{{ label(t) }}</span>
                <span v-if="outOfRange(t)" class="text-caption text-grey q-ml-xs">（範圍外）</span>
              </div>
              <div class="tl-track" :style="trackStyle">
                <div class="tl-bar cursor-pointer" :class="barClass(t)" :style="barStyle(t)" @click="$emit('edit', t)">
                  <span class="ellipsis">{{ label(t) }}</span>
                  <q-tooltip anchor="top middle" self="bottom middle" :delay="300" class="bg-grey-9 text-body2" max-width="320px">
                    <div class="text-weight-bold">{{ t.title }}</div>
                    <div v-if="t.description" class="q-mt-xs tl-desc">{{ t.description }}</div>
                    <div class="q-mt-xs">看板：{{ boardOf(t)?.name || '' }}　狀態：{{ columnName(t) }}</div>
                    <div>{{ isoToLocal(t.start_at) || '—' }} ～ {{ isoToLocal(t.due_at) || '—' }}</div>
                  </q-tooltip>
                </div>
              </div>
            </div>
            <div v-if="!scheduled.length" class="tl-row"><div class="tl-label text-grey">沒有已排程事項</div></div>
          </div>
        </div>
      </div>

      <!-- 捲到兩端才出現 -->
      <q-btn v-if="atStart" class="tl-more tl-more-left" unelevated color="primary" icon="chevron_left" label="顯示更多" no-caps
        @click="extend(-1)" />
      <q-btn v-if="atEnd" class="tl-more tl-more-right" unelevated color="primary" icon-right="chevron_right" label="顯示更多" no-caps
        @click="extend(1)" />
    </div>

    <div v-if="unscheduled.length" class="q-mt-md">
      <div class="text-subtitle1 q-mb-xs">未排程 <q-badge color="grey-5">{{ unscheduled.length }}</q-badge></div>
      <q-list bordered separator dense>
        <q-item v-for="t in unscheduled" :key="t.id" clickable :style="labelStyle(t)" @click="$emit('edit', t)">
          <q-item-section avatar><q-badge :color="priorityColor(t.priority)" rounded /></q-item-section>
          <q-item-section :class="{ 'text-strike text-grey': t.completed_at }">{{ label(t) }}</q-item-section>
        </q-item>
      </q-list>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { priorityColor, isoToLocal, textOn } from '../utils/task'

const props = defineProps({
  boards: { type: Array, required: true }, // 單一看板頁傳 [board]，總時程表傳全部
  tasks: { type: Array, required: true }, // 已套用篩選
  showBoard: Boolean // 名稱前加「(看板前兩字)」
})
defineEmits(['edit'])

const LABEL_W = 200
const PPD = 48 // 每天像素寬
const BEFORE = 5
const AFTER = 25
const STEP = 10

const scrollEl = ref(null)
const extraBefore = ref(0)
const extraAfter = ref(0)
const atStart = ref(true)
const atEnd = ref(false)

const DAY = 86400000
const startOfDay = d => { const x = new Date(d); x.setHours(0, 0, 0, 0); return x }
const addDays = (d, n) => new Date(d.getTime() + n * DAY)
const diffDays = (a, b) => Math.round((startOfDay(b) - startOfDay(a)) / DAY)
const today = startOfDay(new Date())
const WEEKDAY = ['日', '一', '二', '三', '四', '五', '六']

const scheduled = computed(() => props.tasks.filter(t => t.start_at || t.due_at))
const unscheduled = computed(() => props.tasks.filter(t => !t.start_at && !t.due_at))
const boardOf = t => props.boards.find(b => b.id === t.board_id)
const columnName = t => boardOf(t)?.columns.find(c => c.id === t.column_id)?.name || ''
const label = t => props.showBoard ? `(${(boardOf(t)?.name || '').slice(0, 2)})${t.title}` : t.title

const rangeStart = computed(() => addDays(today, -(BEFORE + extraBefore.value)))
const rangeDays = computed(() => BEFORE + extraBefore.value + 1 + AFTER + extraAfter.value)
const totalW = computed(() => rangeDays.value * PPD)
const todayX = computed(() => diffDays(rangeStart.value, today) * PPD)
const trackStyle = computed(() => ({ width: totalW.value + 'px', backgroundSize: PPD + 'px 100%' }))

const days = computed(() => Array.from({ length: rangeDays.value }, (_, i) => {
  const d = addDays(rangeStart.value, i)
  return {
    key: i,
    md: `${d.getMonth() + 1}/${d.getDate()}`,
    wd: WEEKDAY[d.getDay()],
    weekend: d.getDay() === 0 || d.getDay() === 6,
    today: diffDays(d, today) === 0
  }
}))

// 事項起訖（沒開始就用到期當天，沒到期就用開始當天）
const span = t => {
  const s = startOfDay(new Date(t.start_at || t.due_at))
  const e = startOfDay(new Date(t.due_at || t.start_at))
  return { s, e: e < s ? s : e }
}

function outOfRange (t) {
  const { s, e } = span(t)
  const end = addDays(rangeStart.value, rangeDays.value - 1)
  return e < rangeStart.value || s > end
}

// 總時程表：長條與左側名稱套用看板顏色；單一看板頁維持優先度色
const boardColor = t => (props.showBoard && !t.completed_at) ? boardOf(t)?.color : null

// 超出範圍的部分由 .tl-track 的 overflow hidden 裁掉
function barStyle (t) {
  const { s, e } = span(t)
  const c = boardColor(t)
  return {
    left: diffDays(rangeStart.value, s) * PPD + 'px',
    width: (diffDays(s, e) + 1) * PPD - 2 + 'px',
    ...(c ? { background: c, color: textOn(c) } : {})
  }
}

function barClass (t) {
  if (boardColor(t)) return ''
  return t.completed_at ? 'bg-grey-5' : `bg-${priorityColor(t.priority)}`
}

// 左側名稱底色：看板色加透明度，淡淡的
function labelStyle (t) {
  const c = boardColor(t)
  return c ? { background: c + '33' } : {}
}

function onScroll () {
  const el = scrollEl.value
  if (!el) return
  atStart.value = el.scrollLeft < 4
  atEnd.value = el.scrollLeft + el.clientWidth >= el.scrollWidth - 4
}

async function extend (dir) {
  const el = scrollEl.value
  const keep = el.scrollLeft
  if (dir < 0) extraBefore.value += STEP
  else extraAfter.value += STEP
  await nextTick()
  // 往前加日期時，畫面停在原本看的位置
  el.scrollLeft = dir < 0 ? keep + STEP * PPD : keep
  onScroll()
}

function scrollToToday () {
  if (scrollEl.value) scrollEl.value.scrollLeft = Math.max(0, todayX.value - 150)
  nextTick(onScroll)
}

onMounted(scrollToToday)
</script>

<style scoped>
.tl-wrap { position: relative; }
.tl-scroll { overflow-x: auto; border: 1px solid #e0e0e0; border-radius: 4px; background: #fff; }
.tl-row { display: flex; align-items: stretch; border-bottom: 1px solid #eee; min-height: 32px; }
.tl-header { position: sticky; top: 0; background: #fafafa; z-index: 2; font-size: 12px; }
.tl-label {
  width: 200px; min-width: 200px; padding: 6px 8px; box-sizing: border-box;
  position: sticky; left: 0; background: #fff; z-index: 3; border-right: 1px solid #e0e0e0;
  display: flex; align-items: center; overflow: hidden; white-space: nowrap;
}
.tl-header .tl-label { background: #fafafa; }
.tl-track {
  position: relative; display: flex; overflow: hidden;
  background-image: linear-gradient(to right, #eee 1px, transparent 1px);
}
.tl-seg { border-right: 1px solid #ddd; padding: 4px 2px; box-sizing: border-box; white-space: nowrap; overflow: hidden; text-align: center; line-height: 1.2; }
.tl-weekend { background: #f5f5f5; }
.tl-is-today { background: #fdecea; font-weight: bold; }
.tl-body { position: relative; }
.tl-today { position: absolute; top: 0; bottom: 0; width: 2px; background: #c10015; z-index: 1; }
.tl-bar {
  position: absolute; top: 5px; height: 22px; border-radius: 4px; color: #fff; font-size: 12px;
  padding: 0 6px; line-height: 22px; overflow: hidden; white-space: nowrap; z-index: 1;
}
.tl-desc { white-space: pre-wrap; opacity: 0.9; }
.tl-more { position: absolute; top: 50%; transform: translateY(-50%); z-index: 4; opacity: 0.9; }
.tl-more-left { left: 208px; }
.tl-more-right { right: 8px; }
</style>
