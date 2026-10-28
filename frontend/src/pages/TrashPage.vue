<template>
  <q-page class="page">
    <div class="row items-end justify-between q-mb-lg" style="gap: 16px">
      <div>
        <h1 class="page-title">回收桶</h1>
        <p class="text-sub q-ma-none" style="font-size: 15px">刪掉的任務會在這裡待 {{ TRASH_KEEP_DAYS }} 天，之後自動永久刪除</p>
      </div>
      <q-btn no-caps unelevated class="fun-btn bg-surface" icon="delete_sweep" label="清空回收桶" :disable="!tasks.trash.length" @click="emptyAll" />
    </div>

    <div v-if="!tasks.trash.length" class="fun-card q-pa-lg text-center text-sub" style="max-width: 480px">回收桶是空的</div>

    <div v-else class="fun-card table-box">
      <table>
        <thead>
          <tr>
            <th scope="col">標題</th>
            <th scope="col">分類</th>
            <th scope="col">刪除時間</th>
            <th scope="col">自動刪除</th>
            <th scope="col" class="text-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in tasks.trash" :key="t.id">
            <td class="text-weight-bold">{{ t.title }}</td>
            <td><span class="cat-chip" :style="{ background: cats.label(t.category_id).color }">{{ cats.label(t.category_id).name }}</span></td>
            <td class="text-sub nowrap">{{ formatDateTime(t.deleted_at) }}</td>
            <td><span class="left" :class="{ urgent: trashDaysLeft(t.deleted_at) <= 5 }">剩 {{ trashDaysLeft(t.deleted_at) }} 天</span></td>
            <td>
              <div class="row justify-end no-wrap" style="gap: 8px">
                <q-btn no-caps unelevated class="fun-btn flat-fun bg-mint" icon="undo" label="還原" @click="restore(t)" />
                <q-btn no-caps unelevated class="fun-btn flat-fun bg-surface" label="永久刪除" @click="purge(t)" />
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </q-page>
</template>

<script setup>
import { onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useTasksStore } from '../stores/tasks'
import { useCategoriesStore } from '../stores/categories'
import { formatDateTime, trashDaysLeft, TRASH_KEEP_DAYS } from '../utils/task'

const $q = useQuasar()
const tasks = useTasksStore()
const cats = useCategoriesStore()

function confirm (title, message, ok) {
  return $q.dialog({
    title, message,
    cancel: { label: '取消', flat: true, noCaps: true },
    ok: { label: ok, noCaps: true, unelevated: true, color: 'negative' }
  })
}

async function restore (t) {
  const dropped = await tasks.restore(t.id)
  if (dropped) {
    $q.notify({ type: 'warning', message: `已還原「${t.title}」，但有 ${dropped} 個時段在刪除期間被其他任務佔用，沒有還原`, timeout: 6000 })
  } else {
    $q.notify({ type: 'positive', message: `已還原「${t.title}」` })
  }
}

function purge (t) {
  confirm('永久刪除', `「${t.title}」刪除後就救不回來了，確定嗎？`, '永久刪除').onOk(() => tasks.purge(t.id))
}

function emptyAll () {
  confirm('清空回收桶', `回收桶裡的 ${tasks.trash.length} 個任務會全部永久刪除，確定嗎？`, '全部刪除').onOk(() => tasks.purgeAll())
}

onMounted(() => Promise.all([tasks.loadTrash(), cats.load()]))
</script>

<style scoped>
.page { padding: 32px 40px 48px; }
@media (max-width: 600px) { .page { padding: 20px 16px 32px; } }
.table-box { overflow-x: auto; }
table { width: 100%; min-width: 720px; border-collapse: collapse; font-size: 15px; }
th { text-align: left; padding: 16px 12px; color: var(--sub); font-size: 13px; }
td { padding: 12px; border-top: 2px dashed var(--grid); }
th:first-child, td:first-child { padding-left: 20px; }
th:last-child, td:last-child { padding-right: 20px; }
.nowrap { white-space: nowrap; }
.left { display: inline-block; padding: 4px 10px; border-radius: 10px; background: var(--soft); white-space: nowrap; }
.left.urgent { background: var(--danger); color: var(--on-color); font-weight: 700; }
</style>
