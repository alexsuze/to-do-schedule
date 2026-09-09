import { defineStore } from 'pinia'
import api from '../services/api'

const LAST_BOARD_KEY = 'lastBoardId'

// 目前看板與篩選條件（依需求書 5.2）；檢視模式存在各看板的 last_view_mode
// 最後開啟的看板：localStorage 供即時使用，並同步到後端 settings.last_board_id（換裝置也記得）
export const useUiStore = defineStore('ui', {
  state: () => ({
    currentBoardId: localStorage.getItem(LAST_BOARD_KEY),
    // 三種檢視共用；due 為 dueBucket 的值（overdue / today / week / later / none）
    filters: { priority: null, due: null, tags: [], search: '' }
  }),
  getters: {
    filtering: (s) => !!(s.filters.priority || s.filters.due || s.filters.tags?.length || (s.filters.search || '').trim())
  },
  actions: {
    setCurrentBoard (id) {
      if (id === this.currentBoardId) return
      this.currentBoardId = id
      if (id) localStorage.setItem(LAST_BOARD_KEY, id)
      else localStorage.removeItem(LAST_BOARD_KEY)
      api.put('/settings', { last_board_id: id || '' }).catch(() => {})
    },
    // 沒有本機紀錄時，以後端設定為準
    async restoreLastBoard () {
      if (this.currentBoardId) return
      try {
        const { data } = await api.get('/settings')
        if (data.last_board_id) this.currentBoardId = data.last_board_id
      } catch { /* 忽略，退回第一個看板 */ }
    },
    clearFilters () {
      this.filters = { priority: null, due: null, tags: [], search: '' }
    }
  }
})
