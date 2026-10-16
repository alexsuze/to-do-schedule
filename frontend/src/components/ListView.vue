<template>
  <div>
    <div class="row items-center q-gutter-sm q-mb-sm">
      <q-btn color="primary" icon="add" label="新增事項" @click="add" />
      <q-select v-model="groupBy" :options="GROUPS" label="分組" dense outlined clearable emit-value map-options
        style="min-width: 150px" />
    </div>

    <div v-for="g in groups" :key="g.key" class="q-mb-md">
      <div v-if="groupBy" class="text-subtitle1 q-mb-xs">
        {{ g.label }} <q-badge color="grey-5">{{ g.rows.length }}</q-badge>
      </div>
      <q-table
        flat bordered dense
        :rows="g.rows"
        :columns="columns"
        row-key="id"
        :pagination="{ rowsPerPage: 0 }"
        hide-pagination
        binary-state-sort
        no-data-label="沒有事項"
      >
        <template #body-cell-done="p">
          <q-td :props="p">
            <q-checkbox :model-value="!!p.row.completed_at" dense @update:model-value="v => setDone(p.row, v)" />
          </q-td>
        </template>
        <template #body-cell-title="p">
          <q-td :props="p" class="cursor-pointer" :class="{ 'q-pl-lg': p.row.parent_id }" @click="$emit('edit', p.row)">
            <span v-if="p.row.parent_id" class="text-grey">↳ </span>
            <q-icon v-if="isMeeting(p.row)" name="event" size="xs" class="q-mr-xs" />
            <span :class="{ 'text-strike text-grey': p.row.completed_at }">{{ p.row.title }}</span>
            <!-- 分組後階段可能不在所屬任務下方，補上任務名稱 -->
            <span v-if="p.row.parent_id && groupBy" class="text-caption text-grey q-ml-xs">（{{ store.byId(p.row.parent_id)?.title }}）</span>
          </q-td>
        </template>
        <template #body-cell-status="p">
          <q-td :props="p">
            <q-select :model-value="p.row.column_id" :options="columnOptions" dense borderless emit-value map-options
              @update:model-value="v => store.move(p.row.id, { columnId: v })" />
          </q-td>
        </template>
        <template #body-cell-priority="p">
          <q-td :props="p">
            <q-select :model-value="p.row.priority" :options="PRIORITIES" dense borderless emit-value map-options
              @update:model-value="v => store.patch(p.row, { priority: v })">
              <template #selected>
                <q-badge :color="priorityColor(p.row.priority)">{{ priorityLabel(p.row.priority) }}</q-badge>
              </template>
            </q-select>
          </q-td>
        </template>
        <template #body-cell-due="p">
          <q-td :props="p" :class="dueClass(p.row)">{{ isoToLocal(p.row.due_at) }} {{ stateLabel(p.row) }}</q-td>
        </template>
        <template #body-cell-tags="p">
          <q-td :props="p">
            <q-chip v-for="tag in p.row.tags" :key="tag" dense size="sm" color="grey-3">{{ tag }}</q-chip>
          </q-td>
        </template>
      </q-table>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useQuasar } from 'quasar'
import { useTasksStore } from '../stores/tasks'
import { PRIORITIES, DUE_BUCKETS, priorityColor, dueState, dueBucket, stateLabel, isMeeting, isoToLocal, nestStages } from '../utils/task'

const props = defineProps({
  board: { type: Object, required: true },
  tasks: { type: Array, required: true } // 已套用篩選
})
defineEmits(['edit'])

const $q = useQuasar()
const store = useTasksStore()

const GROUPS = [
  { label: '依狀態', value: 'status' },
  { label: '依優先度', value: 'priority' },
  { label: '依到期日', value: 'due' }
]
const groupBy = ref(null)

const columnOptions = computed(() => props.board.columns.map(c => ({ label: c.name, value: c.id })))
const columnName = id => props.board.columns.find(c => c.id === id)?.name || ''
const priorityLabel = p => PRIORITIES.find(x => x.value === p)?.label || p
const priorityRank = p => PRIORITIES.findIndex(x => x.value === p)

const columns = [
  { name: 'done', label: '', field: 'completed_at', align: 'center', style: 'width: 40px' },
  { name: 'title', label: '標題', field: 'title', align: 'left', sortable: true },
  { name: 'status', label: '狀態', field: r => columnName(r.column_id), align: 'left', sortable: true, style: 'width: 140px' },
  { name: 'priority', label: '優先度', field: 'priority', align: 'left', sortable: true, style: 'width: 100px',
    sort: (a, b) => priorityRank(a) - priorityRank(b) },
  { name: 'start', label: '開始', field: r => isoToLocal(r.start_at), align: 'left', sortable: true },
  { name: 'due', label: '到期 / 結束', field: 'due_at', format: v => isoToLocal(v), align: 'left', sortable: true },
  { name: 'tags', label: '標籤', field: 'tags', align: 'left' }
]

const groups = computed(() => {
  const all = props.tasks
  if (!groupBy.value) return [{ key: 'all', label: '', rows: nestStages(all) }]
  let defs
  if (groupBy.value === 'status') defs = props.board.columns.map(c => ({ key: c.id, label: c.name, pick: t => t.column_id === c.id }))
  else if (groupBy.value === 'priority') defs = PRIORITIES.map(p => ({ key: p.value, label: p.label, pick: t => t.priority === p.value }))
  else defs = DUE_BUCKETS.map(b => ({ key: b.value, label: b.label, pick: t => dueBucket(t) === b.value }))
  return defs.map(d => ({ ...d, rows: nestStages(all.filter(d.pick)) })).filter(g => g.rows.length)
})

function dueClass (t) {
  return { overdue: 'text-negative text-weight-bold', soon: 'text-warning text-weight-bold', ended: 'text-grey-5' }[dueState(t)] || ''
}

function add () {
  $q.dialog({
    title: '新增事項',
    prompt: { model: '', type: 'text', label: '標題', maxlength: 100 },
    cancel: true
  }).onOk(title => store.create(title))
}

// 勾選完成 = 移到第一個完成類型欄位；取消 = 移回第一個非完成欄位
function setDone (t, done) {
  const target = props.board.columns.find(c => c.is_done_type === done)
  if (!target) {
    $q.notify({ type: 'warning', message: done ? '此看板沒有完成類型的欄位' : '此看板沒有非完成類型的欄位' })
    return
  }
  store.move(t.id, { columnId: target.id })
}
</script>
