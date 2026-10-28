<template>
  <q-page class="page">
    <div class="row items-end justify-between q-mb-lg" style="gap: 16px">
      <div>
        <h1 class="page-title">任務</h1>
        <p class="text-sub q-ma-none" style="font-size: 15px">{{ openCount }} 個未完成，一張一張來就好</p>
      </div>
      <div class="row items-center" style="gap: 16px">
        <q-btn-toggle
          v-model="filter"
          class="fun-toggle"
          no-caps unelevated
          toggle-color="primary" toggle-text-color="dark"
          :options="FILTERS"
          aria-label="篩選狀態"
        />
        <q-btn no-caps unelevated class="fun-btn bg-accent-fun" icon="add" label="新增任務" @click="open(null)" />
      </div>
    </div>

    <div v-if="!shown.length" class="fun-card q-pa-lg text-center text-sub" style="max-width: 480px">
      {{ filter === 'open' ? '沒有未完成的任務，按右上角新增一張吧！' : '這裡空空的' }}
    </div>

    <div class="grid">
      <article
        v-for="(t, i) in shown" :key="t.id"
        class="fun-card task-card"
        :class="{ done: t.done }"
        :style="{ transform: `rotate(${ROT[i % 3]})` }"
        tabindex="0"
        @click="open(t)" @keydown.enter="open(t)"
      >
        <div class="row items-center no-wrap">
          <span class="cat-chip" :style="{ background: cats.label(t.category_id).color }">{{ cats.label(t.category_id).name }}</span>
          <q-space />
          <q-btn
            round unelevated
            class="check-btn" :class="t.done ? 'bg-mint' : 'bg-surface'"
            :icon="t.done ? 'check' : undefined"
            :aria-label="t.done ? '已完成，點擊改回未完成' : '標記完成'"
            @click.stop="tasks.setDone(t, !t.done)"
          />
        </div>
        <h3 class="title">{{ t.title }}</h3>
        <span v-if="t.deadline" class="deadline" :class="deadlineState(t)">
          <q-icon name="schedule" size="16px" />{{ deadlineLabel(t) }}
        </span>
        <div v-if="subtaskProgress(t).total" class="column q-gutter-y-xs">
          <div class="row justify-between text-sub" style="font-size: 13px">
            <span>子任務</span>
            <span class="font-round text-weight-bold">{{ subtaskProgress(t).done }} / {{ subtaskProgress(t).total }}</span>
          </div>
          <div class="bar"><div :style="{ width: pct(t) }" /></div>
        </div>
        <div class="hours text-sub">
          <q-icon name="timer" size="16px" />累計工時
          <strong class="font-round">{{ formatHours(blocks.minutesByTask[t.id] || 0) }}</strong>
        </div>
      </article>
    </div>

    <TaskDialog v-model="dialog" :task="editing" />
  </q-page>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useTasksStore } from '../stores/tasks'
import { useCategoriesStore } from '../stores/categories'
import { useBlocksStore } from '../stores/blocks'
import { deadlineState, deadlineLabel, sortTasks, subtaskProgress } from '../utils/task'
import { formatHours } from '../utils/timeline'
import TaskDialog from '../components/TaskDialog.vue'

const tasks = useTasksStore()
const cats = useCategoriesStore()
const blocks = useBlocksStore()

const FILTERS = [
  { label: '未完成', value: 'open' },
  { label: '已完成', value: 'done' },
  { label: '全部', value: 'all' }
]
const ROT = ['-0.6deg', '0.5deg', '0deg'] // 卡片微微歪斜，童趣感
const filter = ref('open')

const shown = computed(() => sortTasks(tasks.tasks.filter(t =>
  filter.value === 'all' || (filter.value === 'done') === t.done)))
const openCount = computed(() => tasks.tasks.filter(t => !t.done).length)

function pct (t) {
  const p = subtaskProgress(t)
  return `${Math.round(p.done / p.total * 100)}%`
}

const dialog = ref(false)
const editing = ref(null)
function open (t) {
  editing.value = t
  dialog.value = true
}

onMounted(() => Promise.all([tasks.load(), cats.load(), blocks.load()]))
</script>

<style scoped>
.page { padding: 32px 40px 48px; }
@media (max-width: 600px) { .page { padding: 20px 16px 32px; } }
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 24px;
}
.task-card {
  display: flex; flex-direction: column; gap: 12px;
  padding: 18px; cursor: pointer;
  transition: transform .15s;
}
.task-card:hover, .task-card:focus-visible { transform: rotate(0) translateY(-3px) !important; outline: none; }
.task-card.done { opacity: .7; }
.task-card.done .title { text-decoration: line-through; }
.title { margin: 0; font-size: 19px; line-height: 1.35; font-weight: 700; }
.check-btn { width: 44px; height: 44px; border: 2px solid var(--line); }
.deadline {
  align-self: flex-start; display: flex; align-items: center; gap: 6px;
  padding: 4px 10px; border-radius: 10px; font-size: 14px;
  background: var(--soft); color: var(--ink);
}
.deadline.soon { background: var(--butter); color: var(--on-color); font-weight: 700; }
.deadline.overdue { background: var(--danger); color: var(--on-color); font-weight: 700; }
.bar { height: 12px; border-radius: 999px; border: 2px solid var(--line); background: var(--soft); overflow: hidden; }
.bar > div { height: 100%; background: var(--mint); }
.hours { display: flex; align-items: center; gap: 6px; padding-top: 8px; border-top: 2px dashed var(--grid); font-size: 14px; }
.hours strong { color: var(--ink); }
</style>
