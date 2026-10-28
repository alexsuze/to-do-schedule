<template>
  <q-page class="page">
    <div class="row items-end justify-between q-mb-lg" style="gap: 16px">
      <div>
        <h1 class="page-title">時程表</h1>
        <p class="text-sub q-ma-none" style="font-size: 15px">把左邊的任務拖進格子，拉下緣調整工時</p>
      </div>
      <div class="row items-center" style="gap: 10px">
        <span class="week-total">本週已排 <span class="font-round">{{ formatHours(weekMinutes) }}</span></span>
        <q-btn round unelevated class="fun-btn bg-surface" icon="chevron_left" aria-label="上一週" @click="shiftWeek(-7)" />
        <span class="font-round text-weight-bold text-center" style="font-size: 18px; min-width: 128px">{{ weekLabel }}</span>
        <q-btn round unelevated class="fun-btn bg-surface" icon="chevron_right" aria-label="下一週" @click="shiftWeek(7)" />
        <q-btn no-caps unelevated class="fun-btn bg-accent-fun" label="本週" @click="ws = weekStart(new Date())" />
      </div>
    </div>

    <div class="body">
      <aside class="pool fun-card" aria-label="未完成任務">
        <span class="text-weight-bold" style="font-size: 16px">未完成任務</span>
        <q-input v-model="search" dense outlined placeholder="搜尋任務" clearable>
          <template #prepend><q-icon name="search" /></template>
        </q-input>
        <div
          v-for="t in pool" :key="t.id"
          class="pool-item"
          @pointerdown="onPointerDown($event, { taskId: t.id, title: t.title, len: 1, grab: 0 })"
          @contextmenu.prevent
        >
          <q-icon name="drag_indicator" class="text-sub" size="16px" />
          <span class="cat-dot" :style="{ background: cats.label(t.category_id).color }" />
          <span class="ellipsis col" style="font-size: 14px">{{ t.title }}</span>
          <span class="font-round text-sub" style="font-size: 13px">{{ formatHours(blocks.minutesByTask[t.id] || 0) }}</span>
        </div>
        <div v-if="!pool.length" class="text-sub" style="font-size: 14px">{{ search ? '找不到符合的任務' : '沒有未完成任務' }}</div>
        <span class="text-sub" style="font-size: 12px; line-height: 1.5">同一個任務可以拖進去很多次，工時會自動加總。手機請按住約半秒再拖。</span>
      </aside>

      <div ref="gridBox" class="grid-box fun-card">
        <div class="grid-inner">
          <div class="cols">
            <span />
            <div v-for="(d, i) in days" :key="i" class="day-head">
              <span class="wd" :class="{ today: isToday(d), weekend: i >= 5 }">{{ WD[i] }}</span>
              <span class="font-round text-sub" style="font-size: 13px">{{ d.getMonth() + 1 }}/{{ String(d.getDate()).padStart(2, '0') }}</span>
            </div>
          </div>

          <div class="row" style="gap: 8px">
            <q-btn v-if="view.start > 0" no-caps unelevated class="expand col" icon="expand_less" :label="`展開更早 ${EXPAND_HOURS} 小時（${hh(Math.max(0, view.start - EXPAND_HOURS))}–${hh(view.start)}）`" @click="expand(-1)" />
            <q-btn v-if="isExpanded" no-caps unelevated class="expand" icon="unfold_less" label="恢復 09:00–18:00" @click="setRange({ ...DEFAULT_RANGE })" />
          </div>

          <div class="cols">
            <div class="column">
              <div v-for="h in hours" :key="h" class="hour-label font-round text-sub">{{ hh(h) }}</div>
            </div>
            <div
              v-for="(d, i) in days" :key="i"
              class="day-col" :class="{ today: isToday(d) }" :data-day="i"
              :style="{ height: `${rows * ROW}px` }"
            >
              <div
                v-for="b in dayBlocks[i]" :key="b.id"
                class="block" :class="{ done: taskOf(b)?.done, moving: drag?.block?.id === b.id }"
                :style="blockStyle(b)"
                :title="`${taskOf(b)?.title}（${blockRange(b)}）`"
                @pointerdown="onPointerDown($event, { block: b, taskId: b.task_id, title: taskOf(b)?.title, len: lenOf(b), grab: grabSlot($event) })"
                @contextmenu.prevent
                @click="openTask(b)"
              >
                <span class="block-title">{{ taskOf(b)?.title }}</span>
                <span v-if="lenOf(b) >= 2" class="font-round" style="font-size: 12px; opacity: .85">{{ blockRange(b) }}</span>
                <q-btn round flat dense size="xs" icon="close" class="block-del" aria-label="移除時段" @pointerdown.stop @click.stop="removeBlock(b)" />
                <div class="resize" aria-hidden="true" @pointerdown.stop.prevent="startResize($event, b)" @click.stop />
              </div>
              <div v-if="ghost && ghost.day === i" class="ghost" :class="{ bad: ghost.bad }" :style="{ top: `${(ghost.slot - view.start * 2) * ROW + 1}px`, height: `${ghost.len * ROW - 3}px` }" />
              <div v-if="isToday(d) && nowTop !== null" class="now" :style="{ top: `${nowTop}px` }" />
            </div>
          </div>

          <q-btn v-if="view.end < 24" no-caps unelevated class="expand" icon="expand_more" :label="`展開更晚 ${EXPAND_HOURS} 小時（${hh(view.end)}–${hh(Math.min(24, view.end + EXPAND_HOURS))}）`" @click="expand(1)" />
        </div>
      </div>
    </div>

    <!-- 拖曳中跟著手指 / 滑鼠的小卡 -->
    <div v-if="drag" class="float-chip" :style="{ left: `${drag.x + 14}px`, top: `${drag.y + 14}px` }">{{ drag.title }}</div>

    <TaskDialog v-model="dialog" :task="editing" />
  </q-page>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useQuasar } from 'quasar'
import { useTasksStore } from '../stores/tasks'
import { useCategoriesStore } from '../stores/categories'
import { useBlocksStore } from '../stores/blocks'
import { sortTasks } from '../utils/task'
import {
  DEFAULT_RANGE, EXPAND_HOURS, SLOTS_PER_DAY, addDays, blockSlots, formatHours, formatSlot,
  minutesOf, overlaps, sameDay, slotOf, slotTime, visibleRange, weekStart
} from '../utils/timeline'
import TaskDialog from '../components/TaskDialog.vue'

const ROW = 28 // 一格 30 分鐘的高度（px）
const WD = ['一', '二', '三', '四', '五', '六', '日']
const RANGE_KEY = 'timelineRange'

const $q = useQuasar()
const tasks = useTasksStore()
const cats = useCategoriesStore()
const blocks = useBlocksStore()

const hh = h => `${String(h).padStart(2, '0')}:00`

/* 週 */
const ws = ref(weekStart(new Date()))
const days = computed(() => Array.from({ length: 7 }, (_, i) => addDays(ws.value, i)))
const weekLabel = computed(() => {
  const f = d => `${d.getMonth() + 1}/${String(d.getDate()).padStart(2, '0')}`
  return `${f(days.value[0])} – ${f(days.value[6])}`
})
const shiftWeek = n => { ws.value = addDays(ws.value, n) }

const now = ref(new Date())
let timer
const isToday = d => sameDay(d, now.value)

/* 本週時段 */
const weekBlocks = computed(() => {
  const end = addDays(ws.value, 7)
  return blocks.list.filter(b => { const s = new Date(b.start_at); return s >= ws.value && s < end })
})
const dayBlocks = computed(() => days.value.map(d => weekBlocks.value.filter(b => sameDay(new Date(b.start_at), d))))
const weekMinutes = computed(() => weekBlocks.value.reduce((n, b) => n + minutesOf(b), 0))

/* 顯示範圍：記在瀏覽器；有時段在範圍外時自動撐開 */
function loadRange () {
  try {
    const r = JSON.parse(localStorage.getItem(RANGE_KEY))
    if (r && r.start >= 0 && r.end <= 24 && r.start < r.end) return r
  } catch { /* 用預設 */ }
  return { ...DEFAULT_RANGE }
}
const range = ref(loadRange())
function setRange (r) {
  range.value = r
  try { localStorage.setItem(RANGE_KEY, JSON.stringify(r)) } catch { /* 不記也能用 */ }
}
const view = computed(() => visibleRange(range.value, weekBlocks.value))
const isExpanded = computed(() => range.value.start !== DEFAULT_RANGE.start || range.value.end !== DEFAULT_RANGE.end)
function expand (dir) {
  const r = { start: view.value.start, end: view.value.end }
  if (dir < 0) r.start = Math.max(0, r.start - EXPAND_HOURS)
  else r.end = Math.min(24, r.end + EXPAND_HOURS)
  setRange(r)
}
const rows = computed(() => (view.value.end - view.value.start) * 2)
const hours = computed(() => Array.from({ length: view.value.end - view.value.start }, (_, i) => view.value.start + i))

const nowTop = computed(() => {
  const m = now.value.getHours() * 60 + now.value.getMinutes() - view.value.start * 60
  return m < 0 || m > rows.value * 30 ? null : m / 30 * ROW
})

/* 時段顯示 */
const taskMap = computed(() => Object.fromEntries(tasks.tasks.map(t => [t.id, t])))
const taskOf = b => taskMap.value[b.task_id]

// 拉伸中的時段用暫時長度顯示
const resizing = ref(null)
const lenOf = b => resizing.value?.b.id === b.id ? resizing.value.len : blockSlots(b).len
const blockRange = b => { const s = blockSlots(b).start; return `${formatSlot(s)}–${formatSlot(s + lenOf(b))}` }
function blockStyle (b) {
  const s = blockSlots(b).start
  return {
    top: `${(s - view.value.start * 2) * ROW + 1}px`,
    height: `${lenOf(b) * ROW - 3}px`,
    background: cats.label(taskOf(b)?.category_id).color
  }
}

/*
 * 拖曳：從左側放入新時段，或移動既有時段。
 * 用 pointer 事件自己處理（瀏覽器內建拖放不支援觸控）：
 * 滑鼠移動超過幾 px 就開始拖；觸控要按住一下才開始，直接滑動仍是捲動畫面。
 */
const LONG_PRESS_MS = 350
const MOVE_PX = 6
const EDGE_PX = 48 // 拖到離畫面邊緣多近開始自動捲動

const gridBox = ref(null)
const drag = ref(null) // 拖曳中：{ taskId, title, block?, len, grab, x, y }
const ghost = ref(null) // 放下位置預覽：{ day, slot, len, start, end, bad }
let pending = null // 按下但還沒開始拖
let pressTimer = null
let scrollTimer = null
let suppressClick = false

// 抓在時段的第幾格，移動時保持相對位置（以時段本身為準，不受點到文字影響）
const grabSlot = e => Math.floor((e.clientY - e.currentTarget.getBoundingClientRect().top) / ROW)

function onPointerDown (e, info) {
  if (e.button !== 0 || resizing.value || drag.value) return
  pending = { ...info, x: e.clientX, y: e.clientY, touch: e.pointerType !== 'mouse' }
  if (pending.touch) pressTimer = setTimeout(startDrag, LONG_PRESS_MS)
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
  window.addEventListener('pointercancel', endDrag)
}

const preventScroll = e => e.preventDefault()

function startDrag () {
  drag.value = { ...pending }
  navigator.vibrate?.(20)
  window.addEventListener('touchmove', preventScroll, { passive: false })
  scrollTimer = setInterval(autoScroll, 16)
}

function onPointerMove (e) {
  if (!drag.value) {
    if (Math.hypot(e.clientX - pending.x, e.clientY - pending.y) < MOVE_PX) return
    if (pending.touch) return endDrag() // 手指沒按住就移動 = 捲動畫面
    startDrag()
  }
  drag.value.x = e.clientX
  drag.value.y = e.clientY
  ghost.value = targetAt(e.clientX, e.clientY)
}

// 手指 / 滑鼠下方是哪一天的哪一格
function targetAt (x, y) {
  const col = document.elementFromPoint(x, y)?.closest('.day-col')
  if (!col) return null
  const day = Number(col.dataset.day)
  const { len, grab, block } = drag.value
  const top = y - col.getBoundingClientRect().top
  const slot = Math.min(Math.max(view.value.start * 2 + Math.floor(top / ROW) - grab, 0), SLOTS_PER_DAY - len)
  const start = slotTime(days.value[day], slot)
  const end = slotTime(days.value[day], slot + len)
  return { day, slot, len, start, end, bad: overlaps(blocks.list, start, end, block?.id) }
}

// 拖到邊緣時捲動頁面（上下）與時程表（左右）
function autoScroll () {
  const d = drag.value
  if (!d) return
  if (d.y < EDGE_PX) window.scrollBy(0, -10)
  else if (d.y > window.innerHeight - EDGE_PX) window.scrollBy(0, 10)
  const box = gridBox.value?.getBoundingClientRect()
  if (box && d.y > box.top && d.y < box.bottom) {
    if (d.x < box.left + EDGE_PX) gridBox.value.scrollLeft -= 10
    else if (d.x > box.right - EDGE_PX) gridBox.value.scrollLeft += 10
  }
  ghost.value = targetAt(d.x, d.y)
}

async function onPointerUp () {
  const g = ghost.value
  const d = drag.value
  endDrag()
  if (!d) return
  // 放開後瀏覽器還會送出 click，別讓它打開編輯視窗
  suppressClick = true
  setTimeout(() => { suppressClick = false })
  if (!g) return
  if (g.bad) {
    $q.notify({ type: 'warning', message: '這段時間已經排了其他任務' })
    return
  }
  if (d.block) {
    if (g.start.getTime() !== new Date(d.block.start_at).getTime()) await blocks.update(d.block, g.start, g.end)
  } else {
    await blocks.create(d.taskId, g.start, g.end)
  }
}

function endDrag () {
  clearTimeout(pressTimer)
  clearInterval(scrollTimer)
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
  window.removeEventListener('pointercancel', endDrag)
  window.removeEventListener('touchmove', preventScroll)
  pending = null
  drag.value = null
  ghost.value = null
}

/* 拉下緣調整工時 */
function startResize (e, b) {
  const { start, len } = blockSlots(b)
  resizing.value = { b, len, origLen: len, y: e.clientY, max: SLOTS_PER_DAY - start }
  window.addEventListener('pointermove', onResizeMove)
  window.addEventListener('pointerup', onResizeEnd, { once: true })
}

function onResizeMove (e) {
  const r = resizing.value
  r.len = Math.min(Math.max(r.origLen + Math.round((e.clientY - r.y) / ROW), 1), r.max)
}

async function onResizeEnd () {
  window.removeEventListener('pointermove', onResizeMove)
  const r = resizing.value
  if (!r || r.len === r.origLen) { resizing.value = null; return }
  const day = new Date(r.b.start_at)
  const start = new Date(r.b.start_at)
  const end = slotTime(day, slotOf(day) + r.len)
  try {
    if (overlaps(blocks.list, start, end, r.b.id)) {
      $q.notify({ type: 'warning', message: '這段時間已經排了其他任務' })
    } else {
      await blocks.update(r.b, start, end)
    }
  } finally {
    resizing.value = null
  }
}

async function removeBlock (b) {
  await blocks.remove(b.id)
  $q.notify({ type: 'positive', message: '已移除時段' })
}

/* 點時段開任務 */
const dialog = ref(false)
const editing = ref(null)
function openTask (b) {
  if (suppressClick || resizing.value || !taskOf(b)) return
  editing.value = taskOf(b)
  dialog.value = true
}

/* 左側：未完成任務 */
const search = ref('')
const pool = computed(() => {
  const q = (search.value || '').trim().toLowerCase()
  return sortTasks(tasks.tasks.filter(t => !t.done && (!q || t.title.toLowerCase().includes(q))))
})

onMounted(() => {
  timer = setInterval(() => { now.value = new Date() }, 60 * 1000)
  return Promise.all([tasks.load(), cats.load(), blocks.load()])
})
onUnmounted(() => {
  clearInterval(timer)
  window.removeEventListener('pointermove', onResizeMove)
  endDrag()
})
</script>

<style scoped>
.page { padding: 32px 32px 48px; }
@media (max-width: 600px) { .page { padding: 20px 16px 32px; } }
.week-total {
  padding: 6px 14px; border-radius: 999px; border: 2px solid var(--line);
  background: var(--mint); color: var(--on-color); font-size: 14px; font-weight: 700;
}
.body { display: flex; flex-wrap: wrap; gap: 20px; align-items: flex-start; }

.pool { flex: 1 1 200px; max-width: 100%; display: flex; flex-direction: column; gap: 10px; padding: 14px; }
@media (min-width: 900px) { .pool { flex: 0 0 230px; position: sticky; top: 16px; } }
.pool-item {
  display: flex; align-items: center; gap: 8px; min-height: 44px; padding: 0 10px;
  border-radius: 12px; border: 2px solid var(--line); background: var(--bg);
  box-shadow: 2px 2px 0 var(--edge); cursor: grab; user-select: none;
}
.pool-item:active { cursor: grabbing; }
/* 按住拖曳時不要選字、不要跳出長按選單 */
.pool-item, .block { user-select: none; -webkit-user-select: none; -webkit-touch-callout: none; }
.float-chip {
  position: fixed; z-index: 9000; pointer-events: none; max-width: 200px;
  padding: 6px 12px; border-radius: 12px; border: 2px solid var(--line);
  background: var(--accent); color: var(--on-color); font-size: 14px; font-weight: 700;
  box-shadow: 3px 3px 0 var(--edge); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.grid-box { flex: 999 1 600px; min-width: 0; overflow-x: auto; padding: 12px 12px 14px; }
.grid-inner { min-width: 660px; display: flex; flex-direction: column; gap: 8px; }
.cols { display: grid; grid-template-columns: 52px repeat(7, minmax(0, 1fr)); }
.day-head { display: flex; flex-direction: column; align-items: center; gap: 2px; padding: 4px 0 6px; }
.wd { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 15px; font-weight: 700; }
.wd.weekend { color: var(--sub); }
.wd.today { background: var(--accent); color: var(--on-color); border: 2px solid var(--line); }

.expand { min-height: 40px; border-radius: 12px; border: 2px dashed var(--line); background: var(--soft); color: var(--ink); font-size: 14px; }
.hour-label { height: 56px; padding-right: 8px; text-align: right; font-size: 12px; transform: translateY(-7px); }

.day-col {
  position: relative;
  border-left: 2px solid var(--grid);
  background-image:
    linear-gradient(to bottom, var(--grid) 0, var(--grid) 2px, transparent 2px),
    linear-gradient(to bottom, transparent 27px, var(--soft) 27px, var(--soft) 28px, transparent 28px);
  background-size: 100% 56px;
}
.day-col.today { background-color: rgba(255, 217, 102, .18); }

.block {
  position: absolute; left: 4px; right: 4px; box-sizing: border-box;
  padding: 3px 22px 8px 8px; border-radius: 12px; overflow: hidden;
  display: flex; flex-direction: column; gap: 1px;
  border: 2px solid #2A2633; box-shadow: 3px 3px 0 var(--edge);
  color: var(--on-color); cursor: grab; z-index: 1;
}
.block:hover { z-index: 3; }
.block.moving { opacity: .35; }
.block.done { opacity: .5; }
.block.done .block-title { text-decoration: line-through; }
.block-title { font-size: 13px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.block-del { position: absolute; top: 1px; right: 1px; color: var(--on-color); opacity: 0; }
.block:hover .block-del, .block-del:focus { opacity: 1; }
.resize { position: absolute; left: 0; right: 0; bottom: 0; height: 10px; cursor: ns-resize; touch-action: none; }
.resize::after {
  content: ''; position: absolute; left: 50%; bottom: 3px; width: 26px; height: 4px; margin-left: -13px;
  border-radius: 4px; background: #2A2633; opacity: .45;
}
/* 觸控裝置沒有 hover：移除鈕常駐、拉把加大 */
@media (hover: none) {
  .block-del { opacity: 1; }
  .resize { height: 18px; }
}
.ghost {
  position: absolute; left: 4px; right: 4px; box-sizing: border-box; z-index: 2; pointer-events: none;
  border: 2px dashed var(--line); border-radius: 12px; background: rgba(142, 227, 184, .45);
}
.ghost.bad { background: rgba(255, 143, 143, .55); }
.now { position: absolute; left: 0; right: 0; height: 0; border-top: 3px solid #E8484E; z-index: 4; pointer-events: none; }
.now::before { content: ''; position: absolute; left: -5px; top: -6.5px; width: 10px; height: 10px; border-radius: 50%; background: #E8484E; }
</style>
