<template>
  <q-dialog v-model="open" @hide="$emit('close')">
    <q-card style="width: 720px; max-width: 95vw">
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">編輯事項</div>
        <q-space />
        <q-btn flat round dense icon="close" v-close-popup />
      </q-card-section>

      <q-card-section class="q-gutter-y-md">
        <q-input v-model="form.title" label="標題 *" maxlength="100" autofocus
          :rules="[v => !!v?.trim() || '標題不可為空白']" />
        <q-input v-model="form.description" label="描述" type="textarea" autogrow />

        <div class="row items-center q-gutter-x-md">
          <div class="text-subtitle2">性質</div>
          <q-btn-toggle v-model="form.type" :options="TYPES" no-caps dense unelevated toggle-color="primary" />
          <span class="text-caption text-grey">{{ meeting ? '提醒以開始時間為準' : '提醒以到期時間為準' }}</span>
        </div>

        <div class="row q-col-gutter-md">
          <q-select class="col-12 col-sm-6" v-model="form.priority" :options="PRIORITIES" label="優先度"
            emit-value map-options />
          <q-select class="col-12 col-sm-6" v-model="form.column_id" :options="columnOptions" label="狀態（欄位）"
            emit-value map-options />
        </div>

        <div class="row q-col-gutter-md">
          <DateTimeField class="col-12 col-sm-6" v-model="form.start_local" :label="meeting ? '開始時間 *' : '開始時間'"
            default-time="09:00" :hint="meeting && !form.start_local ? '會議必須有開始時間' : ''" />
          <DateTimeField class="col-12 col-sm-6" v-model="form.due_local" :label="meeting ? '結束時間' : '到期時間'"
            default-time="18:00" :hint="pastDueHint" />
        </div>

        <div>
          <div class="text-subtitle2 q-mb-xs">
            待辦清單
            <span v-if="form.checklist.length" class="text-caption text-grey">
              （{{ form.checklist.filter(i => i.done).length }}/{{ form.checklist.length }}）
            </span>
          </div>
          <!-- 拖曳把手排序；儲存時依目前順序送出 -->
          <draggable :list="form.checklist" item-key="_key" handle=".item-handle">
            <template #item="{ element: item, index: i }">
              <div class="row items-center no-wrap q-gutter-x-xs">
                <q-icon name="drag_indicator" class="item-handle cursor-move text-grey-5" />
                <q-checkbox v-model="item.done" dense />
                <q-input v-model="item.text" dense borderless class="col" maxlength="100"
                  :class="{ 'text-strike text-grey': item.done }" @keyup.enter="focusNewItem" />
                <q-btn flat dense round size="sm" icon="close" @click="form.checklist.splice(i, 1)" />
              </div>
            </template>
          </draggable>
          <q-input ref="newItemInput" v-model="newItem" dense outlined placeholder="新增待辦項目，按 Enter 加入" maxlength="100"
            class="q-mt-xs" @keyup.enter="addItem">
            <template #prepend><q-icon name="add" /></template>
          </q-input>
        </div>

        <q-select v-model="form.tags" label="標籤" multiple use-input use-chips new-value-mode="add-unique"
          hide-dropdown-icon hint="輸入後按 Enter 新增" />

        <q-select v-model="form.board_id" :options="boardOptions" label="移至其他看板"
          emit-value map-options />

        <div>
          <div class="text-subtitle2 q-mb-xs">Email 通知（{{ meeting ? '開始前' : '到期前' }}）</div>
          <q-option-group v-model="form.notify_mode" inline
            :options="[{ label: '沿用全域設定', value: 'default' }, { label: '自訂', value: 'custom' }, { label: '不通知', value: 'off' }]" />
          <q-select v-if="form.notify_mode === 'custom'" v-model="form.lead_times" :options="LEAD_TIME_OPTIONS"
            label="此事項的提前時間（可複選）" multiple use-chips emit-value map-options dense />
        </div>
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
import { LEAD_TIME_OPTIONS } from '../stores/settings'
import { PRIORITIES, TYPES, isoToLocal, localToIso } from '../utils/task'
import draggable from 'vuedraggable'
import DateTimeField from './DateTimeField.vue'

const props = defineProps({ task: { type: Object, default: null } })
const emit = defineEmits(['close'])

const $q = useQuasar()
const boards = useBoardsStore()
const tasks = useTasksStore()

const open = ref(false)
const saving = ref(false)
const form = ref({})
const newItem = ref('')
const newItemInput = ref(null)

function addItem () {
  const text = newItem.value.trim()
  if (!text) return
  form.value.checklist.push({ text, done: false, _key: Date.now() })
  newItem.value = ''
}
function focusNewItem () { newItemInput.value?.focus() }

watch(() => props.task, t => {
  if (!t) { open.value = false; return }
  form.value = {
    title: t.title,
    description: t.description || '',
    priority: t.priority,
    type: t.type || 'task',
    column_id: t.column_id,
    board_id: t.board_id,
    start_local: isoToLocal(t.start_at),
    due_local: isoToLocal(t.due_at),
    tags: [...(t.tags || [])],
    checklist: (t.checklist || []).map((i, k) => ({ ...i, _key: k })), // _key 供拖曳識別，後端會忽略
    notify_mode: !t.notification_override ? 'default' : (t.notification_override.enabled ? 'custom' : 'off'),
    lead_times: [...(t.notification_override?.lead_times || [])]
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

const meeting = computed(() => form.value.type === 'meeting')

const pastDueHint = computed(() => {
  const iso = localToIso(form.value.due_local)
  return iso && new Date(iso) < Date.now() ? (meeting.value ? '結束時間已過，仍可儲存' : '到期時間已過，仍可儲存') : ''
})

// 會議：選了開始、還沒有結束時，結束預設開始 + 1 小時
watch(() => form.value.start_local, v => {
  if (!meeting.value || !v || form.value.due_local) return
  const iso = localToIso(v)
  if (!iso) return
  const d = new Date(iso); d.setHours(d.getHours() + 1)
  form.value.due_local = isoToLocal(d.toISOString())
})

async function save () {
  if (!form.value.title?.trim()) return
  if (meeting.value && !localToIso(form.value.start_local)) {
    $q.notify({ type: 'negative', message: '會議必須有開始時間' })
    return
  }
  addItem() // 輸入框還有字就一併加入
  saving.value = true
  try {
    const t = props.task
    await tasks.update(t.id, {
      title: form.value.title,
      description: form.value.description,
      priority: form.value.priority,
      type: form.value.type,
      start_at: localToIso(form.value.start_local),
      due_at: localToIso(form.value.due_local),
      tags: form.value.tags,
      checklist: form.value.checklist.map(({ text, done }) => ({ text, done })),
      notification_override: notificationOverride()
    })
    if (form.value.board_id !== t.board_id || form.value.column_id !== t.column_id) {
      await tasks.move(t.id, { boardId: form.value.board_id, columnId: form.value.column_id })
    }
    open.value = false
  } finally {
    saving.value = false
  }
}

// null = 沿用全域；{enabled:false} = 不通知；{enabled:true, lead_times} = 自訂
function notificationOverride () {
  const m = form.value.notify_mode
  if (m === 'default') return null
  return { enabled: m === 'custom', lead_times: m === 'custom' ? form.value.lead_times : [] }
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
