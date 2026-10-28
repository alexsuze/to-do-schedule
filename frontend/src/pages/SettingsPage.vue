<template>
  <q-page class="page column q-gutter-y-lg" style="max-width: 860px">
    <div>
      <h1 class="page-title">設定</h1>
      <p class="text-sub q-ma-none" style="font-size: 15px">外觀、分類和每日摘要信</p>
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
                <div class="palette">
                  <button
                    v-for="p in PALETTE" :key="p" v-close-popup type="button"
                    class="swatch" :style="{ background: p }"
                    :aria-label="`顏色 ${p}`" :aria-pressed="String(p === c.color)"
                    @click="p !== c.color && cats.update(c, { color: p })"
                  >
                    <q-icon v-if="p === c.color" name="check" size="20px" color="dark" />
                  </button>
                </div>
              </q-menu>
            </button>
            <span class="col" style="font-size: 16px">{{ c.name }}</span>
            <span class="text-sub" style="font-size: 13px">{{ countOf(c.id) }} 個任務</span>
            <q-btn flat round icon="edit" :aria-label="`改名「${c.name}」`" @click="renaming = { c, name: c.name }" />
            <q-btn flat round icon="delete_outline" :aria-label="`刪除「${c.name}」`" @click="remove(c)" />
          </div>
        </template>
      </draggable>
      <div class="row no-wrap" style="gap: 8px">
        <q-input v-model="newName" dense outlined class="col" placeholder="新分類名稱，例如：專案A" maxlength="30" @keydown.enter="add" />
        <q-btn no-caps unelevated class="fun-btn bg-accent-fun" icon="add" label="新增分類" @click="add" />
      </div>
    </section>

    <section class="fun-card q-pa-lg" style="display: flex; flex-direction: column; gap: 16px" aria-label="Email 每日摘要">
      <div class="row items-center justify-between" style="gap: 12px">
        <div>
          <h2 class="section-title">Email 每日摘要</h2>
          <div class="text-sub">今天到期、已逾期、今天排的時段，一封信看完。都沒有就不寄。</div>
        </div>
        <q-toggle v-model="mail.digest_enabled" color="primary" :label="mail.digest_enabled ? '已開啟' : '已關閉'" class="text-weight-bold" />
      </div>
      <div class="mail-grid">
        <q-input v-model="mail.email" outlined type="email" label="收件 Email" hint="只能寄到 Resend 帳號的 Email" />
        <q-input v-model="mail.digest_time" outlined type="time" label="寄送時間" stack-label hint="台北時間（Asia/Taipei）" />
      </div>
      <div class="row items-center justify-between" style="gap: 12px">
        <span class="text-sub" style="font-size: 13px">{{ lastResult ? `上次：${lastResult}` : '還沒寄過摘要' }}</span>
        <div class="row" style="gap: 10px">
          <q-btn no-caps unelevated class="fun-btn bg-surface" label="寄送測試信" :loading="sending" @click="sendTest" />
          <q-btn no-caps unelevated class="fun-btn bg-accent-fun" label="儲存" :loading="savingMail" @click="saveMail" />
        </div>
      </div>
    </section>

    <!-- 分類改名；有改過沒儲存就關閉時會先問 -->
    <q-dialog :model-value="!!renaming" @update:model-value="onRenameToggle">
      <q-card v-if="renaming" style="width: 420px; max-width: 92vw; padding: 24px" class="column" :style="{ gap: '16px' }">
        <div class="font-round text-weight-bold" style="font-size: 22px">分類改名</div>
        <q-input v-model="renaming.name" label="分類名稱" outlined autofocus maxlength="30" @keydown.enter="saveRename" />
        <div class="row justify-end" style="gap: 10px">
          <q-btn flat no-caps label="取消" @click="onRenameToggle(false)" />
          <q-btn no-caps unelevated class="fun-btn bg-accent-fun" label="儲存" @click="saveRename" />
        </div>
      </q-card>
    </q-dialog>
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
import { resolveUnsaved, useUnsavedGuard } from '../utils/unsaved'

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

/* 改名：renaming = { c, name } */
const renaming = ref(null)
const renameDirty = () => !!renaming.value && renaming.value.name.trim() !== renaming.value.c.name

// 成功（或沒改）回傳 true；名稱空白回傳 false；API 失敗會丟錯
async function saveRename () {
  const r = renaming.value
  const name = r.name.trim()
  if (!name) {
    $q.notify({ type: 'warning', message: '請輸入分類名稱' })
    return false
  }
  if (name !== r.c.name) await cats.update(r.c, { name })
  renaming.value = null
  return true
}

async function onRenameToggle (open) {
  if (open) return
  if (!renameDirty() || await resolveUnsaved(saveRename)) renaming.value = null
}

/* Email 每日摘要 */
const pickMail = s => ({ email: s.email || '', digest_enabled: !!s.digest_enabled, digest_time: s.digest_time || '08:00' })
const mail = ref(pickMail({}))
const savedMail = ref(JSON.stringify(mail.value))
const lastResult = ref('')
const savingMail = ref(false)
const mailDirty = () => JSON.stringify(mail.value) !== savedMail.value

function applySettings (s) {
  mail.value = pickMail(s)
  savedMail.value = JSON.stringify(mail.value)
  lastResult.value = s.last_digest_result || ''
}

// 成功回傳 true；API 失敗會丟錯
async function saveMail () {
  savingMail.value = true
  try {
    applySettings((await api.put('/settings', mail.value)).data)
    $q.notify({ type: 'positive', message: '已儲存每日摘要設定' })
    return true
  } finally { savingMail.value = false }
}

const sending = ref(false)
async function sendTest () {
  if (mailDirty()) {
    $q.notify({ type: 'warning', message: '請先儲存，測試信會寄到已儲存的 Email' })
    return
  }
  sending.value = true
  try {
    await api.post('/settings/test-email')
    $q.notify({ type: 'positive', message: `已寄出測試信到 ${mail.value.email}，請收信` })
  } finally { sending.value = false }
}

/* 換頁時：還沒按新增的分類名稱、還沒儲存的改名、還沒儲存的摘要設定 */
useUnsavedGuard(
  () => !!newName.value.trim() || renameDirty() || mailDirty(),
  async () => {
    if (renameDirty() && !(await saveRename())) return false
    if (newName.value.trim()) await add()
    if (mailDirty()) await saveMail()
    return true
  },
  () => newName.value.trim() && !renameDirty() && !mailDirty()
    ? `「${newName.value.trim()}」還沒按新增分類，要怎麼處理？`
    : undefined
)

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

onMounted(() => Promise.all([cats.load(), tasks.load(), api.get('/settings').then(r => applySettings(r.data))]))
</script>

<style scoped>
.page { padding: 32px 40px 48px; }
@media (max-width: 600px) { .page { padding: 20px 16px 32px; } }
.section-title { margin: 0 0 4px; font-size: 20px; font-weight: 700; line-height: 1.4; }
.mail-grid { display: flex; flex-wrap: wrap; align-items: flex-start; gap: 16px; }
.mail-grid > * { flex: 1 1 220px; }
.cat-row { min-height: 52px; padding: 0 6px 0 10px; border-radius: 16px; border: 2px solid var(--line); background: var(--bg); }
.swatch {
  width: 32px; height: 32px; flex-shrink: 0; border-radius: 10px; border: 2px solid #2A2633; cursor: pointer; padding: 0;
  display: flex; align-items: center; justify-content: center;
}
.palette { display: grid; grid-template-columns: repeat(4, 36px); gap: 8px; padding: 12px; }
.palette .swatch { width: 36px; height: 36px; }
</style>
