<template>
  <q-page padding>
    <div class="row items-center q-mb-md q-gutter-sm">
      <div class="text-h5">回收桶</div>
      <q-btn-toggle v-model="scope" unelevated toggle-color="primary" size="sm"
        :options="[{ label: '目前看板', value: 'board' }, { label: '全部看板', value: 'all' }]" />
      <q-space />
      <q-btn flat color="negative" icon="delete_forever" label="清空回收桶" :disable="!items.length && !deletedBoards.length" @click="empty" />
    </div>
    <div class="text-caption text-grey q-mb-md">已刪除的資料保留 30 天，逾期自動永久刪除。</div>

    <q-table
      flat bordered dense
      :rows="items"
      :columns="columns"
      row-key="id"
      :loading="loading"
      :pagination="{ rowsPerPage: 0 }"
      hide-pagination
      no-data-label="回收桶是空的"
    >
      <template #body-cell-board="p">
        <q-td :props="p">
          {{ p.row.board_name }}
          <q-badge v-if="p.row.board_deleted" color="grey" class="q-ml-xs">看板已刪除</q-badge>
        </q-td>
      </template>
      <template #body-cell-actions="p">
        <q-td :props="p" class="text-right">
          <q-btn flat dense size="sm" color="primary" icon="restore" label="復原" @click="restore(p.row)" />
          <q-btn flat dense size="sm" color="negative" icon="delete_forever" label="永久刪除" @click="purge(p.row)" />
        </q-td>
      </template>
    </q-table>

    <template v-if="scope === 'all' && deletedBoards.length">
      <div class="text-subtitle1 q-mt-lg q-mb-xs">已刪除的看板</div>
      <q-list bordered separator dense>
        <q-item v-for="b in deletedBoards" :key="b.id">
          <q-item-section avatar><q-icon name="dashboard" :style="{ color: b.color || '#1976d2' }" /></q-item-section>
          <q-item-section>
            {{ b.name }}
            <q-item-label caption>刪除於 {{ isoToLocal(b.deleted_at) }}，剩 {{ daysLeft(b.deleted_at) }} 天</q-item-label>
          </q-item-section>
          <q-item-section side>
            <div>
              <q-btn flat dense size="sm" color="primary" icon="restore" label="復原" @click="restoreBoard(b)" />
              <q-btn flat dense size="sm" color="negative" icon="delete_forever" label="永久刪除" @click="purgeBoard(b)" />
            </div>
          </q-item-section>
        </q-item>
      </q-list>
    </template>
  </q-page>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import api from '../services/api'
import { useBoardsStore } from '../stores/boards'
import { useUiStore } from '../stores/ui'
import { isoToLocal } from '../utils/task'

const $q = useQuasar()
const boards = useBoardsStore()
const ui = useUiStore()

const scope = ref(ui.currentBoardId ? 'board' : 'all')
const items = ref([])
const deletedBoards = ref([])
const loading = ref(false)

const boardParam = computed(() => scope.value === 'board' ? ui.currentBoardId : null)
const daysLeft = deletedAt => Math.max(0, Math.ceil((new Date(deletedAt).getTime() + 30 * 86400000 - Date.now()) / 86400000))

const columns = [
  { name: 'title', label: '標題', field: 'title', align: 'left' },
  { name: 'board', label: '原看板', field: 'board_name', align: 'left' },
  { name: 'column', label: '原欄位', field: 'column_name', align: 'left' },
  { name: 'deleted', label: '刪除時間', field: r => isoToLocal(r.deleted_at), align: 'left' },
  { name: 'left', label: '剩餘天數', field: r => daysLeft(r.deleted_at), align: 'center' },
  { name: 'actions', label: '', field: 'id', align: 'right' }
]

function fail (e) {
  $q.notify({ type: 'negative', message: e.response?.data?.detail || e.message })
}

async function load () {
  loading.value = true
  try {
    const params = boardParam.value ? { board_id: boardParam.value } : {}
    const [t, b] = await Promise.all([api.get('/trash/tasks', { params }), api.get('/trash/boards')])
    items.value = t.data
    deletedBoards.value = b.data
  } catch (e) { fail(e) } finally { loading.value = false }
}
watch(scope, load, { immediate: true })

async function restore (row) {
  try {
    await api.post(`/trash/tasks/${row.id}/restore`)
    $q.notify({ type: 'positive', message: `已復原「${row.title}」` })
    await load()
  } catch (e) { fail(e) }
}

function purge (row) {
  $q.dialog({
    title: '永久刪除',
    message: `確定永久刪除「${row.title}」？此動作無法復原。`,
    cancel: true,
    ok: { label: '永久刪除', color: 'negative' }
  }).onOk(async () => {
    try { await api.delete(`/trash/tasks/${row.id}`); await load() } catch (e) { fail(e) }
  })
}

async function restoreBoard (b) {
  try {
    await api.post(`/trash/boards/${b.id}/restore`)
    $q.notify({ type: 'positive', message: `已復原看板「${b.name}」` })
    await Promise.all([load(), boards.load()])
  } catch (e) { fail(e) }
}

function purgeBoard (b) {
  $q.dialog({
    title: '永久刪除看板',
    message: `確定永久刪除看板「${b.name}」與其回收桶內的事項？此動作無法復原。`,
    cancel: true,
    ok: { label: '永久刪除', color: 'negative' }
  }).onOk(async () => {
    try { await api.delete(`/trash/boards/${b.id}`); await load() } catch (e) { fail(e) }
  })
}

function empty () {
  const all = scope.value === 'all'
  $q.dialog({
    title: '清空回收桶',
    message: all ? '確定永久刪除回收桶內所有事項與看板？此動作無法復原。' : '確定永久刪除此看板回收桶內的所有事項？此動作無法復原。',
    cancel: true,
    ok: { label: '清空', color: 'negative' }
  }).onOk(async () => {
    try {
      await api.delete('/trash', { params: boardParam.value ? { board_id: boardParam.value } : {} })
      await load()
    } catch (e) { fail(e) }
  })
}
</script>
