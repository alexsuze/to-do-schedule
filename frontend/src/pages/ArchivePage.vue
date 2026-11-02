<template>
  <q-page class="page">
    <div class="row items-end justify-between q-mb-lg" style="gap: 16px">
      <div>
        <h1 class="page-title">封存</h1>
        <p class="text-sub q-ma-none" style="font-size: 15px">
          完成超過 {{ tasks.archiveDays }} 天的任務會自動收到這裡（天數可在設定頁調整）；解除封存後重新計算
        </p>
      </div>
      <div class="row items-center" style="gap: 16px">
        <q-input v-model="search" outlined dense clearable placeholder="搜尋任務" aria-label="搜尋任務" style="min-width: 200px">
          <template #prepend><q-icon name="search" /></template>
        </q-input>
        <ProjectFilter v-model="projectFilter" />
      </div>
    </div>

    <div v-if="!shown.length" class="fun-card q-pa-lg text-center text-sub" style="max-width: 480px">
      {{ archived.length ? '沒有符合的任務' : '還沒有封存的任務' }}
    </div>

    <div v-else class="fun-card table-box">
      <table>
        <thead>
          <tr>
            <th scope="col">標題</th>
            <th scope="col">專案 / 分類</th>
            <th scope="col">完成時間</th>
            <th scope="col">累計工時</th>
            <th scope="col" class="text-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in shown" :key="t.id" class="row-link" tabindex="0" @click="open(t)" @keydown.enter="open(t)">
            <td class="text-weight-bold">{{ t.title }}</td>
            <td style="max-width: 240px"><TaskTag :task="t" /></td>
            <td class="text-sub nowrap">{{ formatDateTime(t.completed_at) }}</td>
            <td class="font-round text-weight-bold nowrap">{{ formatHours(blocks.minutesByTask[t.id] || 0) }}</td>
            <td>
              <div class="row justify-end no-wrap">
                <q-btn no-caps unelevated class="fun-btn flat-fun bg-mint" icon="unarchive" label="解除封存" @click.stop="unarchive(t)" />
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <TaskDialog v-model="dialog" :task="editing" />
  </q-page>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useQuasar } from 'quasar'
import { useTasksStore } from '../stores/tasks'
import { useCategoriesStore } from '../stores/categories'
import { useProjectsStore } from '../stores/projects'
import { useBlocksStore } from '../stores/blocks'
import { formatDateTime, isArchived, matchProject } from '../utils/task'
import { formatHours } from '../utils/timeline'
import TaskDialog from '../components/TaskDialog.vue'
import TaskTag from '../components/TaskTag.vue'
import ProjectFilter from '../components/ProjectFilter.vue'

const $q = useQuasar()
const tasks = useTasksStore()
const cats = useCategoriesStore()
const projects = useProjectsStore()
const blocks = useBlocksStore()

const search = ref('')
const projectFilter = ref('all')

// ponytail: 全部任務一次載入在前端篩選，封存累積到幾千筆再改後端分頁
const archived = computed(() => tasks.tasks.filter(t => isArchived(t, tasks.archiveDays)))
const shown = computed(() => {
  const q = (search.value || '').trim().toLowerCase()
  return archived.value
    .filter(t => matchProject(t, projectFilter.value) && (!q || t.title.toLowerCase().includes(q)))
    .sort((a, b) => String(b.completed_at).localeCompare(String(a.completed_at)))
})

async function unarchive (t) {
  await tasks.unarchive(t.id)
  $q.notify({ type: 'positive', message: `已解除封存「${t.title}」，回到任務頁的「已完成」` })
}

const dialog = ref(false)
const editing = ref(null)
function open (t) {
  editing.value = t
  dialog.value = true
}

onMounted(() => Promise.all([tasks.load(), tasks.loadArchiveDays(), cats.load(), projects.load(), blocks.load()]))
</script>

<style scoped>
.page { padding: 32px 40px 48px; }
@media (max-width: 600px) { .page { padding: 20px 16px 32px; } }
.table-box { overflow-x: auto; }
table { width: 100%; min-width: 720px; border-collapse: collapse; font-size: 15px; }
th { text-align: left; padding: 16px 12px; color: var(--sub); font-size: 13px; }
td { padding: 12px; border-top: 2px dashed var(--grid); }
th:first-child, td:first-child { padding-left: 20px; }
th:last-child, td:last-child { padding-right: 20px; }
.nowrap { white-space: nowrap; }
.row-link { cursor: pointer; }
.row-link:hover, .row-link:focus-visible { background: var(--soft); outline: none; }
</style>
