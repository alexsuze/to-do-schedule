import axios from 'axios'

// 所有後端請求皆經由此 instance，自動帶入 API Token
const api = axios.create({
  baseURL: `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api`,
  headers: { 'X-API-Token': import.meta.env.VITE_API_TOKEN || 'dev-token' },
  timeout: 15000
})

export default api
