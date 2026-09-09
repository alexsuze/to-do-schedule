<template>
  <q-page padding>
    <div v-if="!board" class="text-grey">載入中…</div>

    <template v-else>
      <div class="row items-center q-mb-md">
        <div class="text-h5">{{ board.name }}</div>
        <q-space />
        <q-btn-toggle :model-value="mode" unelevated toggle-color="primary" :options="MODES" @update:model-value="setMode" />
      </div>

      <FilterBar />

      <KanbanView v-if="mode === 'kanban'" :board="board" :tasks="visible" :filtering="ui.filtering" @edit="editing = $event" />
      <ListView v-else-if="mode === 'list'" :board="board" :tasks="visible" @edit="editing = $event" />
      <TimelineView v-else :boards="[board]" :tasks="visible" @edit="editing = $event" />
    </template>

    <TaskDialog :task="editing" @close="editing = null" />
  </q-page>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useBoardsStore } from '../stores/boards'
import { useTasksStore } from '../stores/tasks'
import { useUiStore } from '../stores/ui'
import { filterTasks } from '../utils/task'
import FilterBar from '../components/FilterBar.vue'
import KanbanView from '../components/KanbanView.vue'
import ListView from '../components/ListView.vue'
import TimelineView from '../components/TimelineView.vue'
import TaskDialog from '../components/TaskDialog.vue'

const MODES = [
  { label: '看板', value: 'kanban', icon: 'view_kanban' },
  { label: '列表', value: 'list', icon: 'view_list' },
  { label: '時程表', value: 'timeline', icon: 'timeline' }
]

const route = useRoute()
const router = useRouter()
const boards = useBoardsStore()
const tasks = useTasksStore()
const ui = useUiStore()

const board = computed(() => boards.byId(ui.currentBoardId))
// 每個看板各自記住檢視模式（後端 last_view_mode）
const mode = computed(() => board.value?.last_view_mode || 'kanban')
const visible = computed(() => filterTasks(tasks.tasks, ui.filters))
const editing = ref(null)

function setMode (m) {
  if (m && m !== mode.value) boards.update(board.value.id, { last_view_mode: m })
}

// 路由 id 為準；沒有 id 或 id 不存在時導向最後開啟的看板，再不然就第一個
watch(
  () => [route.params.id, boards.loaded],
  async ([id, loaded]) => {
    if (!loaded) return
    if (id && boards.byId(id)) {
      ui.setCurrentBoard(id)
      await tasks.load(id)
      openFromQuery()
      return
    }
    await ui.restoreLastBoard()
    const fallback = boards.byId(ui.currentBoardId) || boards.boards[0]
    if (fallback) router.replace({ name: 'board', params: { id: fallback.id } })
  },
  { immediate: true }
)

// Email 內的連結：#/boards/{id}?task={taskId} 直接開啟編輯視窗
function openFromQuery () {
  const t = route.query.task && tasks.byId(route.query.task)
  if (t) {
    editing.value = t
    router.replace({ name: 'board', params: { id: route.params.id } })
  }
}
</script>
