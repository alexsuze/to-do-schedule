<template>
  <q-page padding>
    <div class="text-h5 q-mb-md">設定</div>
    <div v-if="!form" class="text-grey">載入中…</div>

    <template v-else>
      <q-card flat bordered class="q-mb-md" style="max-width: 720px">
        <q-card-section class="q-gutter-y-md">
          <div class="text-subtitle1">Email 通知</div>
          <q-toggle v-model="form.notifications_enabled" label="啟用 Email 通知" />
          <q-input v-model="form.notify_email" label="收件 Email" type="email"
            hint="未驗證自有網域前，只能寄到註冊 Resend 的 Email" />
          <q-select v-model="form.default_lead_times" :options="LEAD_TIME_OPTIONS" label="預設提前時間（可複選）"
            multiple use-chips emit-value map-options />

          <q-separator />
          <div class="text-subtitle1">每日摘要</div>
          <q-toggle v-model="form.daily_digest_enabled" label="每日寄送當日到期 / 逾期彙整" />
          <div class="row q-col-gutter-md">
            <q-input class="col-12 col-sm-6" v-model="form.daily_digest_time" label="摘要寄送時間" mask="##:##" placeholder="09:00" />
            <q-select class="col-12 col-sm-6" v-model="form.timezone" :options="TIMEZONES" label="時區" />
          </div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat icon="send" label="寄送測試信" :loading="testing" :disable="!form.notify_email" @click="test" />
          <q-btn color="primary" label="儲存" :loading="saving" @click="save" />
        </q-card-actions>
      </q-card>

      <div class="row items-center q-mb-xs">
        <div class="text-subtitle1">通知紀錄（最近 50 筆）</div>
        <q-btn flat dense round icon="refresh" class="q-ml-sm" @click="store.loadNotifications()" />
      </div>
      <q-table
        flat bordered dense
        :rows="store.notifications"
        :columns="logColumns"
        row-key="id"
        :pagination="{ rowsPerPage: 0 }"
        hide-pagination
        no-data-label="尚無通知紀錄"
        style="max-width: 1000px"
      >
        <template #body-cell-status="p">
          <q-td :props="p">
            <q-badge :color="STATUS[p.row.status]?.color || 'grey'">{{ STATUS[p.row.status]?.label || p.row.status }}</q-badge>
            <q-tooltip v-if="p.row.error_message">{{ p.row.error_message }}</q-tooltip>
          </q-td>
        </template>
      </q-table>
    </template>
  </q-page>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useSettingsStore, LEAD_TIME_OPTIONS } from '../stores/settings'
import { isoToLocal } from '../utils/task'

const store = useSettingsStore()
const form = ref(null)
const saving = ref(false)
const testing = ref(false)

// ponytail: 常用時區列幾個就好，需要再加
const TIMEZONES = ['Asia/Taipei', 'Asia/Tokyo', 'Asia/Shanghai', 'Asia/Singapore', 'UTC', 'America/Los_Angeles', 'America/New_York', 'Europe/London']

const STATUS = {
  pending: { label: '待寄送', color: 'grey' },
  sent: { label: '已寄出', color: 'positive' },
  failed: { label: '失敗', color: 'negative' },
  cancelled: { label: '已取消', color: 'grey-5' }
}
const LEAD_LABEL = Object.fromEntries(LEAD_TIME_OPTIONS.map(o => [o.value, o.label]))

const logColumns = [
  { name: 'type', label: '類型', field: r => r.type === 'daily_digest' ? '每日摘要' : '到期提醒', align: 'left' },
  { name: 'subject', label: '主旨', field: 'subject', align: 'left' },
  { name: 'lead', label: '提前', field: r => LEAD_LABEL[r.lead_time] || '', align: 'left' },
  { name: 'scheduled', label: '排程時間', field: r => isoToLocal(r.scheduled_at), align: 'left' },
  { name: 'sent', label: '寄出時間', field: r => isoToLocal(r.sent_at), align: 'left' },
  { name: 'status', label: '狀態', field: 'status', align: 'left' },
  { name: 'retry', label: '重試', field: 'retry_count', align: 'center' }
]

store.load()
store.loadNotifications()
watch(() => store.settings, s => { if (s && !form.value) form.value = { ...s } }, { immediate: true })

async function save () {
  saving.value = true
  try {
    const { notify_email, notifications_enabled, default_lead_times, daily_digest_enabled, daily_digest_time, timezone } = form.value
    await store.save({ notify_email: notify_email || '', notifications_enabled, default_lead_times, daily_digest_enabled, daily_digest_time, timezone })
    form.value = { ...store.settings }
  } finally { saving.value = false }
}

async function test () {
  testing.value = true
  try {
    await save()
    await store.sendTest()
    await store.loadNotifications()
  } finally { testing.value = false }
}
</script>
