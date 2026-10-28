<template>
  <div class="login-page">
    <div class="fun-card login-card">
      <div class="logo"><q-icon name="check" size="40px" /></div>
      <h1 class="page-title" style="font-size: 30px">工作事項管理</h1>
      <p class="text-sub q-ma-none">任務卡 × 時程表 × 工時統計，今天也慢慢來</p>

      <div v-if="message" class="notice" :class="pending ? 'bg-butter' : 'bg-danger'">
        <q-icon :name="pending ? 'hourglass_top' : 'error_outline'" size="22px" />
        <span>{{ message }}</span>
      </div>

      <div ref="buttonEl" class="google-btn" />
      <q-spinner-dots v-if="busy" size="32px" color="primary" />
      <div v-if="!clientId" class="text-negative" style="font-size: 13px">尚未設定 VITE_GOOGLE_CLIENT_ID，無法登入</div>

      <p class="text-sub q-ma-none" style="font-size: 13px; line-height: 1.6">
        第一次登入會送出使用申請，管理員核准後就能開始使用。
      </p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from '../stores/auth'

const GSI_SRC = 'https://accounts.google.com/gsi/client'

const $q = useQuasar()
const router = useRouter()
const auth = useAuthStore()
const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
const buttonEl = ref(null)
const message = ref('')
const pending = ref(false)
const busy = ref(false)

// Google 登入元件只需要載入一次
function loadGsi () {
  if (window.google?.accounts?.id) return Promise.resolve()
  return new Promise((resolve, reject) => {
    const s = document.createElement('script')
    s.src = GSI_SRC
    s.async = true
    s.onload = resolve
    s.onerror = () => reject(new Error('無法載入 Google 登入，請檢查網路後重新整理'))
    document.head.appendChild(s)
  })
}

async function onCredential ({ credential }) {
  busy.value = true
  message.value = ''
  try {
    await auth.login(credential)
    router.replace({ name: 'tasks' })
  } catch (e) {
    pending.value = e.response?.status === 403 && /核准/.test(e.response?.data?.detail || '')
    message.value = e.response?.data?.detail || e.message
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  if (!clientId) return
  try {
    await loadGsi()
    window.google.accounts.id.initialize({ client_id: clientId, callback: onCredential })
    window.google.accounts.id.renderButton(buttonEl.value, {
      theme: $q.dark.isActive ? 'filled_black' : 'outline',
      size: 'large',
      shape: 'pill',
      text: 'signin_with',
      locale: 'zh-TW'
    })
  } catch (e) {
    message.value = e.message
  }
})
</script>

<style scoped>
.login-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 24px 16px; box-sizing: border-box; background: var(--bg); }
.login-card {
  width: 100%; max-width: 420px; box-sizing: border-box; padding: 36px 32px;
  display: flex; flex-direction: column; align-items: center; gap: 16px; text-align: center;
}
.logo {
  width: 72px; height: 72px; border-radius: 22px; border: 2px solid var(--line); box-shadow: 4px 4px 0 var(--edge);
  background: var(--accent); color: var(--on-color); display: flex; align-items: center; justify-content: center;
  transform: rotate(-6deg);
}
.notice {
  width: 100%; box-sizing: border-box; display: flex; align-items: flex-start; gap: 8px; text-align: left;
  padding: 12px 14px; border-radius: 14px; border: 2px solid var(--line); font-size: 14px; line-height: 1.5;
}
.google-btn { min-height: 44px; }
</style>
