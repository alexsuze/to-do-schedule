import { defineStore } from 'pinia'
import api from '../services/api'
import { authToken, clearSession, loadSession, saveSession } from '../services/session'

// 登入狀態（需求書 9.2）
export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: loadSession().user || null,
    pendingCount: 0 // 管理員用：待核准人數（側邊欄徽章）
  }),
  getters: {
    loggedIn: () => !!authToken(),
    isAdmin: (s) => !!s.user?.admin
  },
  actions: {
    // 待核准 / 停用時後端回 403，錯誤往外丟給登入頁顯示
    async login (credential) {
      const { data } = await api.post('/auth/google', { credential })
      saveSession(data.token, data.user)
      this.user = data.user
    },
    // 清掉通行證並重新載入，所有頁面資料一起歸零
    logout () {
      clearSession()
      window.location.hash = '#/login'
      window.location.reload()
    },
    async loadPending () {
      if (!this.isAdmin) return
      const { data } = await api.get('/admin/users')
      this.pendingCount = data.filter(u => u.status === 'pending').length
    }
  }
})
