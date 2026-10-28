<template>
  <q-page class="page column q-gutter-y-lg" style="max-width: 860px">
    <div>
      <h1 class="page-title">設定</h1>
      <p class="text-sub q-ma-none" style="font-size: 15px">外觀、分類和寄信</p>
    </div>

    <section class="fun-card q-pa-lg row items-center justify-between" style="gap: 16px" aria-label="外觀">
      <div>
        <h2 class="section-title">外觀</h2>
        <div class="text-sub">選擇會記在這台電腦的瀏覽器裡</div>
      </div>
      <q-btn-toggle
        :model-value="$q.dark.isActive"
        class="fun-toggle"
        no-caps unelevated
        toggle-color="primary" toggle-text-color="dark"
        :options="[{ label: '淺色', value: false, icon: 'light_mode' }, { label: '深色', value: true, icon: 'dark_mode' }]"
        aria-label="外觀模式"
        @update:model-value="setDark"
      />
    </section>

    <section class="fun-card q-pa-lg column q-gutter-y-md" aria-label="分類管理">
      <div>
        <h2 class="section-title">分類管理</h2>
        <div class="text-sub">刪除分類後，裡面的任務會變成「未分類」，任務本身不會被刪掉</div>
      </div>
      <draggable :list="rows" item-key="id" handle=".drag-handle" class="column q-gutter-y-sm" @end="onDrop">
        <template #item="{ element: c }">
          <div class="cat-row row items-center no-wrap" style="gap: 8px">
            <q-icon name="drag_indicator" class="drag-handle" size="20px" aria-label="拖曳排序" />
            <button type="button" class="swatch" :style="{ background: c.color }" :aria-label="`改變「${c.name}」的顏色`">
              <q-menu anchor="bottom left" self="top left">
                <div class="row q-pa-sm q-gutter-xs" style="width: 196px">
                  <button
                    v-for="p in PALETTE" :key="p" v-close-popup type="button"
                    class="swatch" :style="{ background: p }" :aria-label="`顏色 ${p}`"
                    @click="cats.update(c, { color: p })"
                  />
                </div>
              </q-menu>
            </button>
            <span class="col" style="font-size: 16px">{{ c.name }}</span>
            <span class="text-sub" style="font-size: 13px">{{ countOf(c.id) }} 個任務</span>
            <q-btn flat round icon="edit" :aria-label="`改名「${c.name}」`">
              <q-popup-edit v-slot="scope" :model-value="c.name" buttons label-set="儲存" label-cancel="取消" @save="v => rename(c, v)">
                <q-input v-model="scope.value" dense autofocus maxlength="30" @keyup.enter="scope.set" />
              </q-popup-edit>
            </q-btn>
            <q-btn flat round icon="delete_outline" :aria-label="`刪除「${c.name}」`" @click="remove(c)" />
          </div>
        </template>
      </draggable>
      <div class="row no-wrap" style="gap: 8px">
        <q-input v-model="newName" dense outlined class="col" placeholder="新分類名稱，例如：專案A" maxlength="30" @keydown.enter="add" />
        <q-btn no-caps unelevated class="fun-btn bg-accent-fun" icon="add" label="新增分類" @click="add" />
      </div>
    </section>

    <section class="fun-card q-pa-lg column q-gutter-y-md" aria-label="寄信">
      <div>
        <h2 class="section-title">寄信測試</h2>
        <div class="text-sub">每日摘要信會在階段 4 做出來，現在可以先確認寄信設定是否正常。只能寄到 Resend 帳號的 Email。</div>
      </div>
      <div class="row no-wrap" style="gap: 8px">
        <q-input v-model="to" dense outlined type="email" class="col" placeholder="你的 Resend 帳號 Email" />
        <q-btn no-caps unelevated class="fun-btn bg-surface" label="寄送測試信" :loading="sending" :disable="!to" @click="sendTest" />
      </div>
    </section>
  </q-page>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import draggable from 'vuedraggable'
import api from '../services/api'
import { useCategoriesStore } from '../stores/categories'
import { useTasksStore } from '../stores/tasks'
import { PALETTE } from '../utils/task'
import { setDark } from '../utils/theme'

const $q = useQuasar()
const cats = useCategoriesStore()
const tasks = useTasksStore()

// draggable 需要可變陣列，store 變動時重建本地副本
const rows = ref([])
watch(() => cats.list, l => { rows.value = [...l] }, { immediate: true, deep: true })

const countOf = id => tasks.tasks.filter(t => t.category_id === id).length

const newName = ref('')
async function add () {
  const name = newName.value.trim()
  if (!name) return
  await cats.create(name, PALETTE[cats.list.length % PALETTE.length])
  newName.value = ''
}

function rename (c, name) {
  if (name.trim() && name.trim() !== c.name) cats.update(c, { name: name.trim() })
}

function onDrop () {
  cats.reorder(rows.value.map(c => c.id)).catch(() => { rows.value = [...cats.list] })
}

function remove (c) {
  $q.dialog({
    title: '刪除分類',
    message: `確定要刪除「${c.name}」嗎？裡面的 ${countOf(c.id)} 個任務會變成「未分類」。`,
    cancel: { label: '取消', flat: true, noCaps: true },
    ok: { label: '刪除', noCaps: true, unelevated: true, color: 'negative' }
  }).onOk(async () => {
    await cats.remove(c.id)
    tasks.load()
  })
}

const to = ref('')
const sending = ref(false)
async function sendTest () {
  sending.value = true
  try {
    await api.post('/test-email', { to: to.value })
    $q.notify({ type: 'positive', message: '已寄出，請收信' })
  } finally { sending.value = false }
}

onMounted(() => Promise.all([cats.load(), tasks.load()]))
</script>

<style scoped>
.page { padding: 32px 40px 48px; }
@media (max-width: 600px) { .page { padding: 20px 16px 32px; } }
.section-title { margin: 0 0 4px; font-size: 20px; font-weight: 700; line-height: 1.4; }
.cat-row { min-height: 52px; padding: 0 6px 0 10px; border-radius: 16px; border: 2px solid var(--line); background: var(--bg); }
.swatch { width: 32px; height: 32px; flex-shrink: 0; border-radius: 10px; border: 2px solid #2A2633; cursor: pointer; padding: 0; }
</style>
