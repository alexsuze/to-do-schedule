<template>
  <q-page padding class="column items-center q-gutter-md">
    <div class="text-h5">專案骨架已就緒</div>
    <q-btn color="primary" label="測試後端連線" :loading="loading" @click="ping" />
    <q-banner v-if="result" :class="result.ok ? 'bg-positive text-white' : 'bg-negative text-white'">
      {{ result.message }}
    </q-banner>
  </q-page>
</template>

<script setup>
import { ref } from 'vue'
import api from '../services/api'

const loading = ref(false)
const result = ref(null)

async function ping () {
  loading.value = true
  result.value = null
  try {
    const { data } = await api.get('/ping')
    result.value = { ok: true, message: `後端回應成功：${data.time}` }
  } catch (e) {
    const status = e.response?.status
    result.value = {
      ok: false,
      message: status === 401 ? 'API Token 錯誤（401）' : `連線失敗：${e.message}`
    }
  } finally {
    loading.value = false
  }
}
</script>
