<template>
  <div>
    <div class="row justify-end q-mb-sm">
      <q-btn flat dense icon="add" label="新增欄位" @click="addColumn" />
    </div>

    <!-- 欄位可左右拖曳排序 -->
    <draggable
      :list="columns"
      item-key="id"
      handle=".column-handle"
      class="row no-wrap q-gutter-md board-scroll"
      @end="saveColumns(columns)"
    >
      <template #item="{ element: c }">
        <q-card flat bordered class="column-card column">
          <q-card-section class="row items-center q-py-sm column-handle cursor-move" :style="headerStyle(c)">
            <div class="text-subtitle1 ellipsis">{{ c.name }}</div>
            <q-badge color="grey-5" class="q-ml-sm">{{ (lists[c.id] || []).length }}</q-badge>
            <q-chip v-if="c.is_done_type" dense size="sm" color="positive" text-color="white" class="q-ml-sm">完成</q-chip>
            <q-space />
            <q-btn flat dense round icon="more_vert">
              <q-menu auto-close>
                <q-list dense style="min-width: 160px">
                  <q-item clickable @click="renameColumn(c)"><q-item-section>重新命名</q-item-section></q-item>
                  <q-item clickable @click="coloring = c"><q-item-section>變更底色</q-item-section></q-item>
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

          <!-- 卡片可在欄內排序或拖到其他欄；篩選中順序不可靠，停用拖曳 -->
          <draggable
            :list="lists[c.id]"
            item-key="id"
            group="cards"
            :disabled="filtering"
            class="col q-pa-sm card-list"
            @change="evt => onCardChange(evt, c.id)"
          >
            <template #item="{ element: t }">
              <q-card
                flat bordered
                class="q-mb-sm cursor-pointer task-card"
                :class="{ 'task-done': dueState(t) === 'done' }"
                @click="$emit('edit', t)"
              >
                <div class="priority-bar" :class="`bg-${priorityColor(t.priority)}`" />
                <q-card-section class="q-pa-sm">
                  <div class="text-body2">{{ t.title }}</div>
                  <div v-if="t.parent_id" class="text-caption text-grey-7 ellipsis">↳ {{ store.byId(t.parent_id)?.title }}</div>
                  <div v-else-if="stage(t).stages.length" class="text-caption text-primary">
                    <q-icon name="linear_scale" size="xs" />
                    <template v-if="stage(t).current">
                      目前：{{ stage(t).current.title }}（{{ stage(t).done }}/{{ stage(t).stages.length }}）
                      <span v-if="anchorAt(stage(t).current)">· {{ formatDue(anchorAt(stage(t).current)) }}</span>
                    </template>
                    <template v-else>階段全部完成（{{ stage(t).done }}/{{ stage(t).stages.length }}）</template>
                  </div>
                  <div class="row items-center q-gutter-xs q-mt-xs">
                    <span v-if="anchorAt(t)" class="text-caption" :class="dueClass(t)">
                      <q-icon :name="isMeeting(t) ? 'event' : 'schedule'" size="xs" /> {{ formatDue(anchorAt(t)) }}
                      {{ stateLabel(t) }}
                    </span>
                    <span v-if="t.checklist?.length" class="text-caption"
                      :class="t.checklist.every(i => i.done) ? 'text-positive' : 'text-grey-7'">
                      <q-icon name="checklist" size="xs" /> {{ t.checklist.filter(i => i.done).length }}/{{ t.checklist.length }}
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

    <ColorDialog :show="!!coloring" :model-value="coloring?.color" title="欄位底色"
      @pick="c => saveColumns(columns.map(x => x.id === coloring.id ? { ...x, color: c } : x))" @close="coloring = null" />
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import draggable from 'vuedraggable'
import { useBoardsStore } from '../stores/boards'
import { useTasksStore } from '../stores/tasks'
import { priorityColor, formatDue, dueState, stateLabel, anchorAt, isMeeting, textOn, stageInfo, showOnKanban } from '../utils/task'
import ColorDialog from './ColorDialog.vue'

const props = defineProps({
  board: { type: Object, required: true },
  tasks: { type: Array, required: true }, // 已套用篩選
  filtering: Boolean
})
defineEmits(['edit'])

const $q = useQuasar()
const boards = useBoardsStore()
const store = useTasksStore()

// draggable 需要可變陣列，欄位與各欄卡片都用本地副本，資料變動時重建
const columns = ref([])
const lists = reactive({})
const adding = ref(null)
const newTitle = ref('')
const coloring = ref(null)

function headerStyle (c) {
  return c.color ? { background: c.color, color: textOn(c.color) } : {}
}

watch(() => props.board.columns, cols => { columns.value = [...(cols || [])] }, { immediate: true, deep: true })
// 階段只顯示目前那一個；判斷用未篩選的全部事項
watch(() => [props.tasks, props.board.columns], () => {
  for (const c of props.board.columns || []) {
    lists[c.id] = props.tasks.filter(t => t.column_id === c.id && showOnKanban(store.tasks, t))
      .sort((a, b) => a.order - b.order)
  }
}, { immediate: true, deep: true })

const stage = t => stageInfo(store.tasks, t.id)

function dueClass (t) {
  return { overdue: 'text-negative text-weight-bold', soon: 'text-warning text-weight-bold', ended: 'text-grey-5' }[dueState(t)] || 'text-grey-7'
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
  const t = await store.create(title)
  // 快速新增落在第一欄；若是在別欄新增，補一次移動
  if (t.column_id !== columnId) await store.move(t.id, { columnId })
  newTitle.value = ''
}

// 欄內有隱藏的階段時，畫面位置 ≠ 後端位置：改以「放在哪張可見卡片之前」換算
function onCardChange (evt, columnId) {
  const e = evt.added || evt.moved
  if (!e) return
  const next = lists[columnId][e.newIndex + 1]
  const index = next
    ? store.byColumn(columnId).filter(t => t.id !== e.element.id).findIndex(t => t.id === next.id)
    : null
  store.move(e.element.id, { columnId, index })
}

/* ---------- 欄位 ---------- */
function saveColumns (cols, removedTasks = null) {
  return boards.saveColumns(props.board.id, cols, removedTasks)
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

function toggleDone (c) {
  saveColumns(columns.value.map(x => x.id === c.id ? { ...x, is_done_type: !x.is_done_type } : x))
}

function removeColumn (c) {
  const rest = columns.value.filter(x => x.id !== c.id)
  const count = store.byColumn(c.id).length
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
    await store.load(props.board.id)
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
