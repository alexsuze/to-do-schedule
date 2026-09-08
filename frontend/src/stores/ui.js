import { defineStore } from 'pinia'

// 目前看板、檢視模式、篩選條件（依需求書 5.2）
export const useUiStore = defineStore('ui', {
  state: () => ({
    currentBoardId: null,
    viewMode: 'kanban', // kanban | timeline | list
    filters: { priority: null, dueRange: null, tags: [] }
  })
})
