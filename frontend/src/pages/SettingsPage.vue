<template>
  <q-page class="page column q-gutter-y-lg" style="max-width: 860px">
    <div>
      <h1 class="page-title">設定</h1>
      <p class="text-sub q-ma-none" style="font-size: 15px">外觀、專案與分類、每日摘要信</p>
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

    <section class="fun-card q-pa-lg column q-gutter-y-md" aria-label="專案管理">
      <div>
        <h2 class="section-title">專案管理</h2>
        <div class="text-sub">專案 → 分類 → 任務，工時以專案統計。做完的專案可以封存：選單裡不再出現，但工時和統計都保留。</div>
      </div>
      <draggable :list="projectRows" item-key="id" handle=".drag-handle" class="column q-gutter-y-sm" @end="onProjectDrop">
        <template #item="{ element: p }">
          <div class="cat-row row items-center no-wrap" :class="{ archived: p.archived }" style="gap: 8px">
            <q-icon name="drag_indicator" class="drag-handle" size="20px" aria-label="拖曳排序" />
            <ColorPicker :model-value="p.color" :label="`改變「${p.name}」的顏色`" @pick="color => projects.update(p, { color })" />
            <span class="col ellipsis" style="font-size: 16px">{{ p.name }}<span v-if="p.archived" class="archived-tag">已封存</span></span>
            <span class="text-sub nowrap" style="font-size: 13px">{{ projectCount(p.id) }} 個任務</span>
            <q-btn flat round icon="edit" :aria-label="`改名「${p.name}」`" @click="startRename('project', p)" />
            <q-btn flat round :icon="p.archived ? 'unarchive' : 'inventory_2'" :aria-label="p.archived ? `取消封存「${p.name}」` : `封存「${p.name}」`" @click="projects.update(p, { archived: !p.archived })">
              <q-tooltip>{{ p.archived ? '取消封存' : '封存' }}</q-tooltip>
            </q-btn>
            <q-btn flat round icon="delete_outline" :aria-label="`刪除「${p.name}」`" @click="removeProject(p)" />
          </div>
        </template>
      </draggable>
      <div class="row no-wrap" style="gap: 8px">
        <q-input v-model="newProjectName" dense outlined class="col" placeholder="新專案名稱，例如：樂天" maxlength="30" @keydown.enter="addProject" />
        <q-btn no-caps unelevated class="fun-btn bg-accent-fun" icon="add" label="新增專案" @click="addProject" />
      </div>
    </section>

    <section class="fun-card q-pa-lg column q-gutter-y-md" aria-label="分類管理">
      <div>
        <h2 class="section-title">分類管理</h2>
        <div class="text-sub">分類掛在專案底下。移到其他專案時，分類裡的任務會跟著移過去；刪除分類後任務變成「未分類」，不會被刪掉。</div>
      </div>
      <div v-if="!projects.list.length" class="text-sub">先新增一個專案，才能建立分類</div>
      <div v-for="g in catGroups" :key="g.project.id" class="column" style="gap: 8px">
        <div class="row items-center no-wrap text-weight-bold" style="gap: 8px; font-size: 15px">
          <span class="cat-dot" :style="{ background: g.project.color }" />{{ g.project.name }}
          <span v-if="g.project.archived" class="archived-tag">已封存</span>
        </div>
        <div v-if="!g.rows.length" class="text-sub q-pl-md" style="font-size: 13px">這個專案還沒有分類</div>
        <draggable :list="g.rows" item-key="id" handle=".drag-handle" :group="g.project.id" class="column q-gutter-y-sm" @end="onCategoryDrop">
          <template #item="{ element: c }">
            <div class="cat-row row items-center no-wrap" style="gap: 8px">
              <q-icon name="drag_indicator" class="drag-handle" size="20px" aria-label="拖曳排序" />
              <ColorPicker :model-value="c.color" :label="`改變「${c.name}」的顏色`" @pick="color => cats.update(c, { color })" />
              <span class="col ellipsis" style="font-size: 16px">{{ c.name }}</span>
              <span class="text-sub nowrap" style="font-size: 13px">{{ countOf(c.id) }} 個任務</span>
              <q-btn flat round icon="drive_file_move" :aria-label="`把「${c.name}」移到其他專案`" :disable="projects.list.length < 2">
                <q-tooltip>移到其他專案</q-tooltip>
                <q-menu anchor="bottom right" self="top right">
                  <q-list style="min-width: 180px">
                    <q-item-label header>移到…</q-item-label>
                    <q-item v-for="p in projects.list.filter(x => x.id !== c.project_id)" :key="p.id" v-close-popup clickable @click="moveCategory(c, p)">
                      <q-item-section avatar><span class="cat-dot" :style="{ background: p.color }" /></q-item-section>
                      <q-item-section>{{ p.name }}{{ p.archived ? '（已封存）' : '' }}</q-item-section>
                    </q-item>
                  </q-list>
                </q-menu>
              </q-btn>
              <q-btn flat round icon="edit" :aria-label="`改名「${c.name}」`" @click="startRename('category', c)" />
              <q-btn flat round icon="delete_outline" :aria-label="`刪除「${c.name}」`" @click="remove(c)" />
            </div>
          </template>
        </draggable>
      </div>
      <div v-if="projects.list.length" class="row no-wrap" style="gap: 8px">
        <q-select v-model="newCatProject" :options="projects.active.map(p => ({ label: p.name, value: p.id }))" emit-value map-options dense outlined label="專案" style="min-width: 140px" />
        <q-input v-model="newName" dense outlined class="col" placeholder="新分類名稱，例如：KM" maxlength="30" @keydown.enter="add" />
        <q-btn no-caps unelevated class="fun-btn bg-accent-fun" icon="add" label="新增分類" @click="add" />
      </div>
    </section>

    <section class="fun-card q-pa-lg" style="display: flex; flex-direction: column; gap: 16px" aria-label="Email 每日摘要">
      <div class="row items-center justify-between" style="gap: 12px">
        <div>
          <h2 class="section-title">Email 每日摘要</h2>
          <div class="text-sub">今天到期、已逾期、今天排的時段，一封信看完。都沒有就不寄。</div>
        </div>
        <q-toggle v-model="mail.digest_enabled" :disable="!available" color="primary" :label="mail.digest_enabled ? '已開啟' : '已關閉'" class="text-weight-bold" />
      </div>
      <div v-if="!available" class="notice bg-soft">
        <q-icon name="info_outline" size="20px" />
        <span>每日摘要目前僅限管理員使用（寄信服務尚未開放寄給其他人）。</span>
      </div>
      <div v-else class="mail-grid">
        <q-input :model-value="email" outlined readonly label="收件 Email" hint="固定寄到你登入用的 Google 帳號" />
        <q-input v-model="mail.digest_time" outlined type="time" label="寄送時間" stack-label hint="台北時間（Asia/Taipei）" />
      </div>
      <div v-if="available" class="row items-center justify-between" style="gap: 12px">
        <span class="text-sub" style="font-size: 13px">{{ lastResult ? `上次：${lastResult}` : '還沒寄過摘要' }}</span>
        <div class="row" style="gap: 10px">
          <q-btn no-caps unelevated class="fun-btn bg-surface" label="寄送測試信" :loading="sending" @click="sendTest" />
          <q-btn no-caps unelevated class="fun-btn bg-accent-fun" label="儲存" :loading="savingMail" @click="saveMail" />
        </div>
      </div>
    </section>

    <!-- 專案 / 分類改名；有改過沒儲存就關閉時會先問 -->
    <q-dialog :model-value="!!renaming" @update:model-value="onRenameToggle">
      <q-card v-if="renaming" style="width: 420px; max-width: 92vw; padding: 24px" class="column" :style="{ gap: '16px' }">
        <div class="font-round text-weight-bold" style="font-size: 22px">{{ renameNoun }}改名</div>
        <q-input v-model="renaming.name" :label="`${renameNoun}名稱`" outlined autofocus maxlength="30" @keydown.enter="saveRename" />
        <div class="row justify-end" style="gap: 10px">
          <q-btn flat no-caps label="取消" @click="onRenameToggle(false)" />
          <q-btn no-caps unelevated class="fun-btn bg-accent-fun" label="儲存" @click="saveRename" />
        </div>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import draggable from 'vuedraggable'
import api from '../services/api'
import { useCategoriesStore } from '../stores/categories'
import { useProjectsStore } from '../stores/projects'
import { useTasksStore } from '../stores/tasks'
import { PALETTE } from '../utils/task'
import { setDark } from '../utils/theme'
import { resolveUnsaved, useUnsavedGuard } from '../utils/unsaved'
import ColorPicker from '../components/ColorPicker.vue'

const $q = useQuasar()
const cats = useCategoriesStore()
const projects = useProjectsStore()
const tasks = useTasksStore()

const countOf = id => tasks.tasks.filter(t => t.category_id === id).length
const projectCount = id => tasks.tasks.filter(t => t.project_id === id).length

/* 專案：draggable 需要可變陣列，store 變動時重建本地副本 */
const projectRows = ref([])
watch(() => projects.list, l => { projectRows.value = [...l] }, { immediate: true, deep: true })

function onProjectDrop () {
  projects.reorder(projectRows.value.map(p => p.id)).catch(() => { projectRows.value = [...projects.list] })
}

const newProjectName = ref('')
async function addProject () {
  const name = newProjectName.value.trim()
  if (!name) return
  const p = await projects.create(name, PALETTE[projects.list.length % PALETTE.length])
  newProjectName.value = ''
  if (!newCatProject.value) newCatProject.value = p.id
}

function removeProject (p) {
  const n = cats.list.filter(c => c.project_id === p.id).length
  $q.dialog({
    title: '刪除專案',
    message: `確定要刪除「${p.name}」嗎？底下的 ${n} 個分類會一併刪除；${projectCount(p.id)} 個任務會保留，改為「無專案」。只是不想再看到的話，建議用「封存」。`,
    cancel: { label: '取消', flat: true, noCaps: true },
    ok: { label: '刪除', noCaps: true, unelevated: true, color: 'negative' }
  }).onOk(async () => {
    await projects.remove(p.id)
    await Promise.all([cats.load(), tasks.load()])
  })
}

/* 分類：依專案分組，各組可拖曳排序；整體順序 = 專案順序依序串起各組 */
const catGroups = ref([])
watch(() => [projects.list, cats.list], () => {
  catGroups.value = projects.list.map(p => ({ project: p, rows: cats.list.filter(c => c.project_id === p.id) }))
}, { immediate: true, deep: true })

function onCategoryDrop () {
  cats.reorder(catGroups.value.flatMap(g => g.rows.map(c => c.id))).catch(() => cats.load())
}

async function moveCategory (c, p) {
  await cats.update(c, { project_id: p.id })
  await tasks.load() // 任務跟著分類換了專案
}

const newName = ref('')
const newCatProject = ref(null)
watch(() => projects.active, a => {
  if (!a.some(p => p.id === newCatProject.value)) newCatProject.value = a[0]?.id || null
}, { immediate: true })
async function add () {
  const name = newName.value.trim()
  if (!name) return
  if (!newCatProject.value) {
    $q.notify({ type: 'warning', message: '請先選擇專案' })
    return false
  }
  await cats.create(name, PALETTE[cats.list.length % PALETTE.length], newCatProject.value)
  newName.value = ''
}

/* 改名：renaming = { kind: 'project' | 'category', item, name } */
const renaming = ref(null)
const renameNoun = computed(() => renaming.value?.kind === 'project' ? '專案' : '分類')
const startRename = (kind, item) => { renaming.value = { kind, item, name: item.name } }
const renameDirty = () => !!renaming.value && renaming.value.name.trim() !== renaming.value.item.name

// 成功（或沒改）回傳 true；名稱空白回傳 false；API 失敗會丟錯
async function saveRename () {
  const r = renaming.value
  const name = r.name.trim()
  if (!name) {
    $q.notify({ type: 'warning', message: `請輸入${renameNoun.value}名稱` })
    return false
  }
  if (name !== r.item.name) await (r.kind === 'project' ? projects : cats).update(r.item, { name })
  renaming.value = null
  return true
}

async function onRenameToggle (open) {
  if (open) return
  if (!renameDirty() || await resolveUnsaved(saveRename)) renaming.value = null
}

/* Email 每日摘要：收件人固定為登入的 Google Email；目前只有管理員可用 */
const pickMail = s => ({ digest_enabled: !!s.digest_enabled, digest_time: s.digest_time || '08:00' })
const mail = ref(pickMail({}))
const savedMail = ref(JSON.stringify(mail.value))
const lastResult = ref('')
const email = ref('')
const available = ref(false)
const savingMail = ref(false)
const mailDirty = () => JSON.stringify(mail.value) !== savedMail.value

function applySettings (s) {
  mail.value = pickMail(s)
  savedMail.value = JSON.stringify(mail.value)
  lastResult.value = s.last_digest_result || ''
  email.value = s.email || ''
  available.value = !!s.digest_available
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
    $q.notify({ type: 'warning', message: '請先儲存設定再寄測試信' })
    return
  }
  sending.value = true
  try {
    await api.post('/settings/test-email')
    $q.notify({ type: 'positive', message: `已寄出測試信到 ${email.value}，請收信` })
  } finally { sending.value = false }
}

/* 換頁時：還沒按新增的專案 / 分類名稱、還沒儲存的改名、還沒儲存的摘要設定 */
useUnsavedGuard(
  () => !!newProjectName.value.trim() || !!newName.value.trim() || renameDirty() || mailDirty(),
  async () => {
    if (renameDirty() && !(await saveRename())) return false
    if (newProjectName.value.trim()) await addProject()
    if (newName.value.trim() && (await add()) === false) return false
    if (mailDirty()) await saveMail()
    return true
  },
  () => {
    const pending = [newProjectName.value.trim(), newName.value.trim()].filter(Boolean)
    return pending.length && !renameDirty() && !mailDirty()
      ? `「${pending.join('」「')}」還沒按新增，要怎麼處理？`
      : undefined
  }
)

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

onMounted(() => Promise.all([projects.load(), cats.load(), tasks.load(), api.get('/settings').then(r => applySettings(r.data))]))
</script>

<style scoped>
.page { padding: 32px 40px 48px; }
@media (max-width: 600px) { .page { padding: 20px 16px 32px; } }
.section-title { margin: 0 0 4px; font-size: 20px; font-weight: 700; line-height: 1.4; }
.notice { display: flex; align-items: flex-start; gap: 8px; padding: 12px 14px; border-radius: 14px; border: 2px dashed var(--line); font-size: 14px; line-height: 1.5; }
.mail-grid { display: flex; flex-wrap: wrap; align-items: flex-start; gap: 16px; }
.mail-grid > * { flex: 1 1 220px; }
.cat-row { min-height: 52px; padding: 0 6px 0 10px; border-radius: 16px; border: 2px solid var(--line); background: var(--bg); }
.cat-row.archived { opacity: .6; }
.archived-tag { margin-left: 8px; padding: 1px 8px; border-radius: 999px; border: 2px solid var(--line); font-size: 11px; font-weight: 700; color: var(--sub); }
.nowrap { white-space: nowrap; }
</style>
