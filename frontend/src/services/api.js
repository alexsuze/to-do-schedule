import axios from 'axios'
import { Notify } from 'quasar'

// 所有後端請求皆經由此 instance，自動帶入 API Token
const api = axios.create({
  baseURL: `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api`,
  headers: { 'X-API-Token': import.meta.env.VITE_API_TOKEN || 'dev-token' },
  timeout: 15000
})

// 統一顯示錯誤：後端中文訊息在 Problem Details 的 detail
api.interceptors.response.use(null, (e) => {
  Notify.create({ type: 'negative', message: e.response?.data?.detail || e.message })
  return Promise.reject(e)
})

export default api
