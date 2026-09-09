import { defineStore } from 'pinia'

const LAST_BOARD_KEY = 'lastBoardId'

// 目前看板與篩選條件（依需求書 5.2）；檢視模式存在各看板的 last_view_mode
// ponytail: 最後開啟的看板先存 localStorage，階段 5 有 settings API 後改存後端 last_board_id
export const useUiStore = defineStore('ui', {
  state: () => ({
    currentBoardId: localStorage.getItem(LAST_BOARD_KEY),
    // 三種檢視共用；due 為 dueBucket 的值（overdue / today / week / later / none）
    filters: { priority: null, due: null, tags: [], search: '' }
  }),
  getters: {
    filtering: (s) => !!(s.filters.priority || s.filters.due || s.filters.tags.length || s.filters.search.trim())
  },
  actions: {
    setCurrentBoard (id) {
      this.currentBoardId = id
      if (id) localStorage.setItem(LAST_BOARD_KEY, id)
      else localStorage.removeItem(LAST_BOARD_KEY)
    },
    clearFilters () {
      this.filters = { priority: null, due: null, tags: [], search: '' }
    }
  }
})
