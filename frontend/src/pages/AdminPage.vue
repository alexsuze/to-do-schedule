<template>
  <q-page class="page">
    <div class="q-mb-lg">
      <h1 class="page-title">管理</h1>
      <p class="text-sub q-ma-none" style="font-size: 15px">核准新申請、停用或重新啟用使用者。停用的人無法登入，資料會保留。</p>
    </div>

    <div class="fun-card table-box">
      <table>
        <thead>
          <tr>
            <th scope="col">使用者</th>
            <th scope="col">狀態</th>
            <th scope="col">申請時間</th>
            <th scope="col">最後登入</th>
            <th scope="col" class="text-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>
              <div class="row items-center no-wrap" style="gap: 10px">
                <q-avatar size="36px" class="avatar">
                  <img v-if="u.picture" :src="u.picture" alt="" referrerpolicy="no-referrer">
                  <span v-else>{{ (u.name || u.email || '?').slice(0, 1) }}</span>
                </q-avatar>
                <div class="column">
                  <span class="text-weight-bold">{{ u.name }}<span v-if="u.admin" class="admin-tag">管理員</span></span>
                  <span class="text-sub" style="font-size: 13px">{{ u.email }}</span>
                </div>
              </div>
            </td>
            <td><span class="status" :class="u.status">{{ STATUS[u.status] }}</span></td>
            <td class="text-sub nowrap">{{ formatDateTime(u.created_at) }}</td>
            <td class="text-sub nowrap">{{ formatDateTime(u.last_login_at) }}</td>
            <td>
              <div v-if="u.id !== auth.user?.id" class="row justify-end no-wrap" style="gap: 8px">
                <q-btn v-if="u.status !== 'active'" no-caps unelevated class="fun-btn flat-fun bg-mint" :label="u.status === 'pending' ? '核准' : '重新啟用'" @click="setStatus(u, 'active')" />
                <q-btn v-if="u.status !== 'disabled'" no-caps unelevated class="fun-btn flat-fun bg-surface" label="停用" @click="disable(u)" />
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </q-page>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useQuasar } from 'quasar'
import api from '../services/api'
import { useAuthStore } from '../stores/auth'
import { formatDateTime } from '../utils/task'

const STATUS = { pending: '待核准', active: '使用中', disabled: '已停用' }

const $q = useQuasar()
const auth = useAuthStore()
const users = ref([])

async function load () {
  users.value = (await api.get('/admin/users')).data
  auth.pendingCount = users.value.filter(u => u.status === 'pending').length
}

async function setStatus (u, status) {
  await api.put(`/admin/users/${u.id}/status`, { status })
  $q.notify({ type: 'positive', message: `${u.name || u.email}：${STATUS[status]}` })
  await load()
}

function disable (u) {
  $q.dialog({
    title: '停用使用者',
    message: `確定要停用「${u.name || u.email}」嗎？對方會立刻被登出，資料會保留，之後可以重新啟用。`,
    cancel: { label: '取消', flat: true, noCaps: true },
    ok: { label: '停用', noCaps: true, unelevated: true, color: 'negative' }
  }).onOk(() => setStatus(u, 'disabled'))
}

onMounted(load)
</script>

<style scoped>
.page { padding: 32px 40px 48px; }
@media (max-width: 600px) { .page { padding: 20px 16px 32px; } }
.table-box { overflow-x: auto; }
table { width: 100%; min-width: 760px; border-collapse: collapse; font-size: 15px; }
th { text-align: left; padding: 16px 12px; color: var(--sub); font-size: 13px; }
td { padding: 12px; border-top: 2px dashed var(--grid); }
th:first-child, td:first-child { padding-left: 20px; }
th:last-child, td:last-child { padding-right: 20px; }
.nowrap { white-space: nowrap; }
.avatar { border: 2px solid var(--line); background: var(--soft); font-weight: 700; }
.admin-tag { margin-left: 6px; padding: 1px 8px; border-radius: 999px; border: 2px solid var(--line); background: var(--accent); color: var(--on-color); font-size: 11px; }
.status { display: inline-block; padding: 3px 10px; border-radius: 10px; border: 2px solid var(--line); font-size: 13px; font-weight: 700; white-space: nowrap; color: var(--on-color); }
.status.pending { background: var(--butter); }
.status.active { background: var(--mint); }
.status.disabled { background: var(--soft); color: var(--sub); }
</style>
