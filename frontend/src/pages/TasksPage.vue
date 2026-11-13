<template>
  <q-page class="page">
    <div class="row items-end justify-between q-mb-lg" style="gap: 16px">
      <div>
        <h1 class="page-title">任務</h1>
        <p class="text-sub q-ma-none" style="font-size: 15px">
          {{ scopeName }}：{{ openCount }} 個未完成，一張一張來就好<span v-if="hiddenCount">（已隱藏 {{ hiddenCount }} 個未開始）</span>
        </p>
      </div>
      <div class="row items-center" style="gap: 16px">
        <q-toggle v-model="showNotStarted" color="primary" label="顯示未開始" class="text-weight-bold" />
        <ProjectFilter v-model="projectFilter" :company="scope" />
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
          <TaskTag :task="t" :show-company="isAll" />
          <q-space />
          <q-btn flat round icon="content_copy" class="q-mr-xs" :aria-label="`複製「${t.title}」`" @click.stop="copy(t)">
            <q-tooltip>複製（帶入公司、專案、分類）</q-tooltip>
          </q-btn>
          <q-btn
            v-if="t.done" flat round icon="inventory_2" class="q-mr-xs" aria-label="立即封存"
            @click.stop="archive(t)"
          >
            <q-tooltip>立即封存</q-tooltip>
          </q-btn>
          <q-btn
            round unelevated
            class="check-btn" :class="t.done ? 'bg-mint' : 'bg-surface'"
            :icon="t.done ? 'check' : undefined"
            :aria-label="t.done ? '已完成，點擊改回未完成' : '標記完成'"
            @click.stop="tasks.setDone(t, !t.done)"
          />
        </div>
        <h3 class="title">{{ t.title }}</h3>
        <span v-if="startLabel(t)" class="deadline not-started">
          <q-icon name="event" size="16px" />{{ startLabel(t) }}
        </span>
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

    <TaskDialog v-model="dialog" :task="editing" :copy-from="copySource" :preset-project-id="presetProject" :preset-company-id="isAll ? null : scope" />
  </q-page>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { useTasksStore } from '../stores/tasks'
import { useCategoriesStore } from '../stores/categories'
import { useProjectsStore } from '../stores/projects'
import { useBlocksStore } from '../stores/blocks'
import { useCompaniesStore } from '../stores/companies'
import { ALL_COMPANIES, inCompany } from '../utils/company'
import { deadlineState, deadlineLabel, isArchived, notStarted, startLabel, matchProject, sortTasks, subtaskProgress } from '../utils/task'
import { formatHours } from '../utils/timeline'
import TaskDialog from '../components/TaskDialog.vue'
import TaskTag from '../components/TaskTag.vue'
import ProjectFilter from '../components/ProjectFilter.vue'

const $q = useQuasar()
const tasks = useTasksStore()
const cats = useCategoriesStore()
const projects = useProjectsStore()
const blocks = useBlocksStore()
const companies = useCompaniesStore()

// 任務頁可選「全部公司」（需求書 12.4）；換公司時專案篩選回到全部
const scope = computed(() => companies.taskScope)
const isAll = computed(() => scope.value === ALL_COMPANIES)
const scopeName = computed(() => isAll.value ? '全部公司' : (companies.current?.name || ''))
watch(scope, () => { projectFilter.value = 'all' })

const FILTERS = [
  { label: '未完成', value: 'open' },
  { label: '已完成', value: 'done' },
  { label: '全部', value: 'all' }
]
const ROT = ['-0.6deg', '0.5deg', '0deg'] // 卡片微微歪斜，童趣感
const filter = ref('open')
const projectFilter = ref('all')
// 篩選單一專案時，新增任務預設帶入該專案
const presetProject = computed(() => ['all', 'none'].includes(projectFilter.value) ? null : projectFilter.value)

// 顯示 / 隱藏未開始的任務，記在這台電腦
const NOT_STARTED_KEY = 'showNotStarted'
const showNotStarted = ref((() => { try { return localStorage.getItem(NOT_STARTED_KEY) !== '0' } catch { return true } })())
watch(showNotStarted, v => { try { localStorage.setItem(NOT_STARTED_KEY, v ? '1' : '0') } catch { /* 不記也能用 */ } })

// 封存的任務只出現在封存頁
const matched = computed(() => tasks.tasks.filter(t =>
  inCompany(t, scope.value) && !isArchived(t, tasks.archiveDays) &&
  (filter.value === 'all' || (filter.value === 'done') === t.done) && matchProject(t, projectFilter.value)))
const hiddenCount = computed(() => showNotStarted.value ? 0 : matched.value.filter(t => notStarted(t)).length)
const shown = computed(() => sortTasks(showNotStarted.value ? matched.value : matched.value.filter(t => !notStarted(t))))
const openCount = computed(() => tasks.tasks.filter(t => !t.done && inCompany(t, scope.value)).length)

function pct (t) {
  const p = subtaskProgress(t)
  return `${Math.round(p.done / p.total * 100)}%`
}

async function archive (t) {
  await tasks.archive(t.id)
  $q.notify({
    type: 'positive', message: `已封存「${t.title}」，可到左側「封存」查看`,
    actions: [{ label: '復原', color: 'white', noCaps: true, handler: () => tasks.unarchive(t.id) }]
  })
}

const dialog = ref(false)
const editing = ref(null)
const copySource = ref(null)
function open (t) {
  editing.value = t
  copySource.value = null
  dialog.value = true
}
// 複製：打開新增視窗，預先帶入公司、專案、分類與「原標題_複製」，按儲存才建立
function copy (t) {
  editing.value = null
  copySource.value = t
  dialog.value = true
}

onMounted(() => Promise.all([tasks.load(), tasks.loadArchiveDays(), cats.load(), projects.load(), blocks.load()]))
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
.deadline.not-started { background: transparent; border: 2px dashed var(--grid); color: var(--sub); }
.deadline.soon { background: var(--butter); color: var(--on-color); font-weight: 700; }
.deadline.overdue { background: var(--danger); color: var(--on-color); font-weight: 700; }
.bar { height: 12px; border-radius: 999px; border: 2px solid var(--line); background: var(--soft); overflow: hidden; }
.bar > div { height: 100%; background: var(--mint); }
.hours { display: flex; align-items: center; gap: 6px; padding-top: 8px; border-top: 2px dashed var(--grid); font-size: 14px; }
.hours strong { color: var(--ink); }
</style>
