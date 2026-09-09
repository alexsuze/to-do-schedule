<template>
  <q-page padding>
    <div v-if="!board" class="text-grey">載入中…</div>

    <template v-else>
      <div class="row items-center q-mb-md">
        <div class="text-h5">{{ board.name }}</div>
        <q-space />
        <q-btn flat icon="add" label="新增欄位" @click="addColumn" />
      </div>

      <!-- 欄位可左右拖曳排序 -->
      <draggable
        :list="columns"
        item-key="id"
        handle=".column-handle"
        class="row no-wrap q-gutter-md board-scroll"
        @end="onColumnDrop"
      >
        <template #item="{ element: c }">
          <q-card flat bordered class="column-card column">
            <q-card-section class="row items-center q-py-sm column-handle cursor-move">
              <div class="text-subtitle1 ellipsis">{{ c.name }}</div>
              <q-badge color="grey-5" class="q-ml-sm">{{ tasks.byColumn(c.id).length }}</q-badge>
              <q-chip v-if="c.is_done_type" dense size="sm" color="positive" text-color="white" class="q-ml-sm">完成</q-chip>
              <q-space />
              <q-btn flat dense round icon="more_vert">
                <q-menu auto-close>
                  <q-list dense style="min-width: 160px">
                    <q-item clickable @click="renameColumn(c)"><q-item-section>重新命名</q-item-section></q-item>
                    <q-item clickable @click="toggleDone(c)">
                      <q-item-section>{{ c.is_done_type ? '取消完成類型' : '設為完成類型' }}</q-item-section>
                    </q-item>
                    <q-separator />
                    <q-item clickable class="text-negative" :disable="columns.length <= 1" @click="removeColumn(c)">
                      <q-item-section>刪除</q-item-section>
                    </q-item>
                  </q-list>
                </q-menu>
              </q-btn>
            </q-card-section>
            <q-separator />

            <!-- 卡片可在欄內排序或拖到其他欄 -->
            <draggable
              :list="lists[c.id]"
              item-key="id"
              group="cards"
              class="col q-pa-sm card-list"
              @change="evt => onCardChange(evt, c.id)"
            >
              <template #item="{ element: t }">
                <q-card
                  flat bordered
                  class="q-mb-sm cursor-pointer task-card"
                  :class="{ 'task-done': dueState(t) === 'done' }"
                  @click="editing = t"
                >
                  <div class="priority-bar" :class="`bg-${priorityColor(t.priority)}`" />
                  <q-card-section class="q-pa-sm">
                    <div class="text-body2">{{ t.title }}</div>
                    <div class="row items-center q-gutter-xs q-mt-xs">
                      <span v-if="t.due_at" class="text-caption" :class="dueClass(t)">
                        <q-icon name="schedule" size="xs" /> {{ formatDue(t.due_at) }}
                      </span>
                      <q-chip v-for="tag in t.tags" :key="tag" dense size="sm" color="grey-3">{{ tag }}</q-chip>
                    </div>
                  </q-card-section>
                </q-card>
              </template>
            </draggable>

            <q-card-section class="q-pa-sm">
              <q-input
                v-if="adding === c.id"
                v-model="newTitle"
                dense autofocus outlined
                placeholder="輸入標題後按 Enter"
                maxlength="100"
                @keyup.enter="submitNew"
                @keyup.esc="adding = null"
                @blur="adding = null"
              />
              <q-btn v-else flat dense no-caps icon="add" label="新增卡片" class="full-width" @click="startAdd(c.id)" />
            </q-card-section>
          </q-card>
        </template>
      </draggable>
    </template>

    <TaskDialog :task="editing" @close="editing = null" />
  </q-page>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import draggable from 'vuedraggable'
import { useBoardsStore } from '../stores/boards'
import { useTasksStore } from '../stores/tasks'
import { useUiStore } from '../stores/ui'
import TaskDialog from '../components/TaskDialog.vue'
import { priorityColor, formatDue, dueState } from '../utils/task'

const $q = useQuasar()
const route = useRoute()
const router = useRouter()
const boards = useBoardsStore()
const tasks = useTasksStore()
const ui = useUiStore()

const board = computed(() => boards.byId(ui.currentBoardId))
// draggable 需要可變陣列，欄位與各欄卡片都用本地副本，資料變動時重建
const columns = ref([])
const lists = reactive({})
const editing = ref(null)
const adding = ref(null)
const newTitle = ref('')

watch(() => board.value?.columns, cols => { columns.value = [...(cols || [])] }, { immediate: true, deep: true })
watch(() => [tasks.tasks, board.value?.columns], () => {
  for (const c of board.value?.columns || []) lists[c.id] = tasks.byColumn(c.id)
}, { immediate: true, deep: true })

// 路由 id 為準；沒有 id 或 id 不存在時導向最後開啟的看板，再不然就第一個
watch(
  () => [route.params.id, boards.loaded],
  ([id, loaded]) => {
    if (!loaded) return
    if (id && boards.byId(id)) {
      ui.setCurrentBoard(id)
      tasks.load(id)
      return
    }
    const fallback = boards.byId(ui.currentBoardId) || boards.boards[0]
    if (fallback) router.replace({ name: 'board', params: { id: fallback.id } })
  },
  { immediate: true }
)

function dueClass (t) {
  return { overdue: 'text-negative text-weight-bold', soon: 'text-warning text-weight-bold' }[dueState(t)] || 'text-grey-7'
}

/* ---------- 卡片 ---------- */
function startAdd (columnId) {
  adding.value = columnId
  newTitle.value = ''
}

async function submitNew () {
  const title = newTitle.value.trim()
  if (!title) return
  const columnId = adding.value
  const t = await tasks.create(title)
  // 快速新增落在第一欄；若是在別欄新增，補一次移動
  if (t.column_id !== columnId) await tasks.move(t.id, { columnId })
  newTitle.value = ''
}

function onCardChange (evt, columnId) {
  const e = evt.added || evt.moved
  if (e) tasks.move(e.element.id, { columnId, index: e.newIndex })
}

/* ---------- 欄位 ---------- */
function saveColumns (cols, removedTasks = null) {
  return boards.saveColumns(board.value.id, cols, removedTasks)
}

function onColumnDrop () {
  saveColumns(columns.value)
}

function addColumn () {
  $q.dialog({
    title: '新增欄位',
    prompt: { model: '', type: 'text', label: '名稱', maxlength: 30 },
    cancel: true
  }).onOk(name => saveColumns([...columns.value, { id: null, name, is_done_type: false }]))
}

function renameColumn (c) {
  $q.dialog({
    title: '重新命名',
    prompt: { model: c.name, type: 'text', label: '名稱', maxlength: 30 },
    cancel: true
  }).onOk(name => saveColumns(columns.value.map(x => x.id === c.id ? { ...x, name } : x)))
}

async function toggleDone (c) {
  await saveColumns(columns.value.map(x => x.id === c.id ? { ...x, is_done_type: !x.is_done_type } : x))
}

function removeColumn (c) {
  const rest = columns.value.filter(x => x.id !== c.id)
  const count = tasks.byColumn(c.id).length
  if (count === 0) {
    $q.dialog({
      title: '刪除欄位',
      message: `確定刪除「${c.name}」？`,
      cancel: true,
      ok: { label: '刪除', color: 'negative' }
    }).onOk(() => saveColumns(rest))
    return
  }
  $q.dialog({
    title: '刪除欄位',
    message: `「${c.name}」內還有 ${count} 個事項，要如何處理？`,
    options: {
      type: 'radio',
      model: rest[0].id,
      items: [
        ...rest.map(x => ({ label: `移至「${x.name}」`, value: x.id })),
        { label: '一併刪除（可於回收桶復原）', value: 'delete' }
      ]
    },
    cancel: true,
    ok: { label: '確定', color: 'negative' }
  }).onOk(async choice => {
    await saveColumns(rest, choice)
    await tasks.load(board.value.id)
  })
}
</script>

<style scoped>
.board-scroll { overflow-x: auto; align-items: flex-start; min-height: 60vh; }
.column-card { width: 280px; min-width: 280px; }
.card-list { min-height: 40px; }
.task-card { position: relative; overflow: hidden; }
.priority-bar { position: absolute; left: 0; top: 0; bottom: 0; width: 4px; }
.task-done { opacity: 0.55; }
.task-done .text-body2 { text-decoration: line-through; }
</style>
