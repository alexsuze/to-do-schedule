<template>
  <!-- 新增 / 編輯任務；task 為 null 時是新增 -->
  <q-dialog :model-value="modelValue" @update:model-value="v => emit('update:modelValue', v)" @before-show="reset">
    <q-card class="dialog-card">
      <div class="row items-center no-wrap">
        <div class="font-round text-weight-bold" style="font-size: 26px">{{ task ? '編輯任務' : '新增任務' }}</div>
        <q-space />
        <q-btn flat round icon="close" aria-label="關閉" v-close-popup />
      </div>

      <div class="dialog-body">
        <q-input v-model="form.title" label="標題" outlined autofocus maxlength="100" />
        <q-input v-model="form.description" label="描述" outlined type="textarea" autogrow input-style="min-height: 72px" />
        <div class="two-col">
          <div>
            <q-select v-model="form.category_id" :options="catOptions" label="分類" outlined emit-value map-options>
              <template #option="{ itemProps, opt }">
                <q-item v-bind="itemProps">
                  <q-item-section avatar><span class="cat-dot" :style="{ background: opt.color }" /></q-item-section>
                  <q-item-section>{{ opt.label }}</q-item-section>
                </q-item>
              </template>
              <template #selected-item="{ opt }">
                <span class="cat-chip" :style="{ background: opt.color }">{{ opt.label }}</span>
              </template>
            </q-select>
          </div>
          <div>
            <DateTimeField v-model="form.deadline" label="死線" default-time="18:00" outlined />
          </div>
        </div>

        <div class="subtasks">
          <div class="row justify-between items-center">
            <span class="text-weight-bold" style="font-size: 16px">子任務</span>
            <span class="font-round text-weight-bold">{{ doneCount }} / {{ form.subtasks.length }}</span>
          </div>
          <draggable :list="form.subtasks" item-key="id" handle=".drag-handle" class="column" style="gap: 10px">
            <template #item="{ element: s, index }">
              <div class="sub-row row items-center no-wrap">
                <q-icon name="drag_indicator" class="drag-handle q-px-xs" size="20px" aria-label="拖曳排序" />
                <q-checkbox v-model="s.done" color="primary" dense />
                <q-input v-model="s.name" dense borderless class="col q-ml-sm" :input-class="s.done ? 'sub-done' : ''" aria-label="子任務名稱" />
                <q-btn flat round dense icon="close" size="sm" aria-label="刪除子任務" @click="form.subtasks.splice(index, 1)" />
              </div>
            </template>
          </draggable>
          <q-input v-model="newSub" dense outlined placeholder="新增子任務，按 Enter 加入" @keydown.enter.prevent="addSub">
            <template #prepend><q-icon name="add" /></template>
          </q-input>
        </div>

        <div v-if="task" class="row items-center text-sub" style="gap: 6px">
          <q-icon name="timer" size="18px" />累計工時
          <strong class="font-round" style="color: var(--ink)">{{ formatHours(blocks.minutesByTask[task.id] || 0) }}</strong>
          （時程表上 {{ blocks.countOf(task.id) }} 個時段）
        </div>
      </div>

      <div class="dialog-actions">
        <q-btn v-if="task" no-caps unelevated class="fun-btn flat-fun bg-surface" icon="delete_outline" label="移到回收桶" @click="remove" />
        <q-space />
        <q-btn v-if="task" no-caps unelevated class="fun-btn flat-fun bg-mint" :label="form.done ? '改回未完成' : '標記完成'" :loading="saving" @click="save(!form.done)" />
        <q-btn no-caps unelevated class="fun-btn bg-accent-fun" label="儲存" :loading="saving" @click="save(form.done)" />
      </div>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useQuasar } from 'quasar'
import draggable from 'vuedraggable'
import DateTimeField from './DateTimeField.vue'
import { useTasksStore } from '../stores/tasks'
import { useCategoriesStore } from '../stores/categories'
import { useBlocksStore } from '../stores/blocks'
import { isoToLocal, localToIso, UNCATEGORIZED_COLOR } from '../utils/task'
import { formatHours } from '../utils/timeline'

const props = defineProps({
  modelValue: Boolean,
  task: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue'])

const $q = useQuasar()
const tasks = useTasksStore()
const cats = useCategoriesStore()
const blocks = useBlocksStore()

const form = ref({ subtasks: [] })
const newSub = ref('')
const saving = ref(false)

const catOptions = computed(() => [
  { label: '未分類', value: null, color: UNCATEGORIZED_COLOR },
  ...cats.list.map(c => ({ label: c.name, value: c.id, color: c.color }))
])
const doneCount = computed(() => form.value.subtasks.filter(s => s.done).length)

// 每次打開都從原任務複製一份，取消就不影響原資料
function reset () {
  const t = props.task
  form.value = {
    title: t?.title || '',
    description: t?.description || '',
    category_id: t?.category_id || null,
    deadline: isoToLocal(t?.deadline),
    subtasks: (t?.subtasks || []).map(s => ({ ...s })),
    done: !!t?.done
  }
  newSub.value = ''
}

function addSub () {
  const name = newSub.value.trim()
  if (!name) return
  form.value.subtasks.push({ id: crypto.randomUUID(), name, done: false })
  newSub.value = ''
}

async function save (done) {
  if (!form.value.title.trim()) {
    $q.notify({ type: 'warning', message: '請輸入標題' })
    return
  }
  addSub() // 還沒按 Enter 的子任務也一起存
  saving.value = true
  try {
    await tasks.save({ ...form.value, id: props.task?.id, deadline: localToIso(form.value.deadline), done })
    emit('update:modelValue', false)
  } finally {
    saving.value = false
  }
}

function remove () {
  $q.dialog({
    title: '移到回收桶',
    message: `確定要把「${props.task.title}」移到回收桶嗎？30 天內可以還原。`,
    cancel: { label: '取消', flat: true, noCaps: true },
    ok: { label: '移到回收桶', noCaps: true, unelevated: true }
  }).onOk(async () => {
    await tasks.remove(props.task.id)
    emit('update:modelValue', false)
  })
}
</script>

<style scoped>
.dialog-card { width: 620px; max-width: 95vw; padding: 24px 28px; display: flex; flex-direction: column; gap: 20px; }
.dialog-body { display: flex; flex-direction: column; gap: 20px; }
.two-col { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
@media (max-width: 600px) {
  .dialog-card { padding: 20px 16px; }
  .two-col { grid-template-columns: minmax(0, 1fr); }
}
.dialog-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; padding-top: 16px; border-top: 2px dashed var(--grid); }
.subtasks { display: flex; flex-direction: column; gap: 12px; padding: 16px; border-radius: 18px; border: 2px dashed var(--line); background: var(--soft); }
.sub-row { min-height: 48px; padding: 0 6px; border-radius: 14px; border: 2px solid var(--line); background: var(--surface); }
:deep(.sub-done) { text-decoration: line-through; color: var(--sub); }
</style>
