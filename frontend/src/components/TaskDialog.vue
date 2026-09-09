<template>
  <q-dialog v-model="open" @hide="$emit('close')">
    <q-card style="width: 560px; max-width: 95vw">
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">編輯事項</div>
        <q-space />
        <q-btn flat round dense icon="close" v-close-popup />
      </q-card-section>

      <q-card-section class="q-gutter-sm">
        <q-input v-model="form.title" label="標題 *" maxlength="100" autofocus dense
          :rules="[v => !!v?.trim() || '標題不可為空白']" />
        <q-input v-model="form.description" label="描述" type="textarea" autogrow dense />

        <div class="row q-col-gutter-sm">
          <q-select class="col-6" v-model="form.priority" :options="PRIORITIES" label="優先度"
            emit-value map-options dense />
          <q-select class="col-6" v-model="form.column_id" :options="columnOptions" label="狀態（欄位）"
            emit-value map-options dense />
        </div>

        <div class="row q-col-gutter-sm">
          <q-input class="col-6" v-model="form.start_date" label="開始日期" type="date" dense clearable />
          <q-input class="col-6" v-model="form.due_local" label="到期時間" type="datetime-local" dense clearable
            :hint="pastDueHint" />
        </div>

        <q-select v-model="form.tags" label="標籤" multiple use-input use-chips new-value-mode="add-unique"
          hide-dropdown-icon dense hint="輸入後按 Enter 新增" />

        <q-select v-model="form.board_id" :options="boardOptions" label="移至其他看板"
          emit-value map-options dense />
      </q-card-section>

      <q-card-actions align="between">
        <q-btn flat color="negative" label="刪除" @click="remove" />
        <div>
          <q-btn flat label="取消" v-close-popup />
          <q-btn color="primary" label="儲存" :loading="saving" @click="save" />
        </div>
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { useBoardsStore } from '../stores/boards'
import { useTasksStore } from '../stores/tasks'
import { PRIORITIES, isoToLocalInput, localInputToIso } from '../utils/task'

const props = defineProps({ task: { type: Object, default: null } })
const emit = defineEmits(['close'])

const $q = useQuasar()
const boards = useBoardsStore()
const tasks = useTasksStore()

const open = ref(false)
const saving = ref(false)
const form = ref({})

watch(() => props.task, t => {
  if (!t) { open.value = false; return }
  form.value = {
    title: t.title,
    description: t.description || '',
    priority: t.priority,
    column_id: t.column_id,
    board_id: t.board_id,
    start_date: t.start_date || '',
    due_local: isoToLocalInput(t.due_at),
    tags: [...(t.tags || [])]
  }
  open.value = true
}, { immediate: true })

const boardOptions = computed(() => boards.boards.map(b => ({ label: b.name, value: b.id })))
// 換看板時，欄位選項跟著目標看板走
const columnOptions = computed(() =>
  (boards.byId(form.value.board_id)?.columns || []).map(c => ({ label: c.name, value: c.id })))
watch(() => form.value.board_id, (id, old) => {
  if (old && id !== old) form.value.column_id = boards.byId(id)?.columns[0]?.id
})

const pastDueHint = computed(() => {
  const iso = localInputToIso(form.value.due_local)
  return iso && new Date(iso) < Date.now() ? '到期時間已過，仍可儲存' : ''
})

async function save () {
  if (!form.value.title?.trim()) return
  saving.value = true
  try {
    const t = props.task
    await tasks.update(t.id, {
      title: form.value.title,
      description: form.value.description,
      priority: form.value.priority,
      start_date: form.value.start_date || null,
      due_at: localInputToIso(form.value.due_local),
      tags: form.value.tags
    })
    if (form.value.board_id !== t.board_id || form.value.column_id !== t.column_id) {
      await tasks.move(t.id, { boardId: form.value.board_id, columnId: form.value.column_id })
    }
    open.value = false
  } finally {
    saving.value = false
  }
}

function remove () {
  $q.dialog({
    title: '刪除事項',
    message: `確定刪除「${props.task.title}」？可於回收桶復原。`,
    cancel: true,
    ok: { label: '刪除', color: 'negative' }
  }).onOk(async () => {
    await tasks.remove(props.task.id)
    open.value = false
  })
}
</script>
