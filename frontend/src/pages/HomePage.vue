<template>
  <q-layout view="hHh lpR fFf">
    <q-header elevated>
      <q-toolbar><q-toolbar-title>工作事項管理</q-toolbar-title></q-toolbar>
    </q-header>
    <q-page-container>
      <q-page padding class="column q-gutter-md" style="max-width: 480px">
        <div>重新設計中。下方可檢查後端與寄信是否正常。</div>
        <q-btn color="primary" label="測試後端連線" :loading="pinging" @click="ping" />
        <q-input v-model="to" label="收件 Email" type="email" outlined dense />
        <q-btn color="secondary" label="寄送測試信" :loading="sending" :disable="!to" @click="sendTest" />
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { ref } from 'vue'
import { Notify } from 'quasar'
import api from '../services/api'

const pinging = ref(false)
const sending = ref(false)
const to = ref('')

const fail = (e) => Notify.create({ type: 'negative', message: e.response?.data?.detail || e.message })

async function ping () {
  pinging.value = true
  try {
    const { data } = await api.get('/ping')
    Notify.create({ type: 'positive', message: `後端正常（${data.time}）` })
  } catch (e) { fail(e) } finally { pinging.value = false }
}

async function sendTest () {
  sending.value = true
  try {
    await api.post('/test-email', { to: to.value })
    Notify.create({ type: 'positive', message: '已寄出，請收信' })
  } catch (e) { fail(e) } finally { sending.value = false }
}
</script>
