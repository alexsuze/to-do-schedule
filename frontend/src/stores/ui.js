import { defineStore } from 'pinia'

const LAST_BOARD_KEY = 'lastBoardId'

// 目前看板、檢視模式、篩選條件（依需求書 5.2）
// ponytail: 最後開啟的看板先存 localStorage，階段 5 有 settings API 後改存後端 last_board_id
export const useUiStore = defineStore('ui', {
  state: () => ({
    currentBoardId: localStorage.getItem(LAST_BOARD_KEY),
    viewMode: 'kanban', // kanban | timeline | list
    filters: { priority: null, dueRange: null, tags: [] }
  }),
  actions: {
    setCurrentBoard (id) {
      this.currentBoardId = id
      if (id) localStorage.setItem(LAST_BOARD_KEY, id)
      else localStorage.removeItem(LAST_BOARD_KEY)
    }
  }
})
