import { defineStore } from 'pinia'
import api from '../services/api'
import { UNCATEGORIZED_COLOR } from '../utils/task'

export const useCategoriesStore = defineStore('categories', {
  state: () => ({
    list: []
  }),
  getters: {
    byId: (s) => (id) => s.list.find(c => c.id === id),
    // 卡片顯示用：找不到就是未分類
    label: (s) => (id) => {
      const c = s.list.find(x => x.id === id)
      return c ? { name: c.name, color: c.color } : { name: '未分類', color: UNCATEGORIZED_COLOR }
    }
  },
  actions: {
    async load () {
      this.list = (await api.get('/categories')).data
    },
    async create (name, color) {
      const { data } = await api.post('/categories', { name, color })
      this.list.push(data)
    },
    async update (c, patch) {
      const { data } = await api.put(`/categories/${c.id}`, { name: c.name, color: c.color, ...patch })
      this.list.splice(this.list.findIndex(x => x.id === c.id), 1, data)
    },
    // 後端會把該分類的任務改成未分類，呼叫端需重新載入任務
    async remove (id) {
      await api.delete(`/categories/${id}`)
      this.list = this.list.filter(c => c.id !== id)
    },
    async reorder (ids) {
      this.list = (await api.put('/categories/reorder', { ids })).data
    }
  }
})
