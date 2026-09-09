import { defineStore } from 'pinia'
import { Notify } from 'quasar'
import api from '../services/api'

// 顯示後端回傳的錯誤訊息（BoardService 會給中文 message）
function notifyError (e) {
  Notify.create({ type: 'negative', message: e.response?.data?.message || e.message })
  throw e
}

export const useBoardsStore = defineStore('boards', {
  state: () => ({
    boards: [],
    loaded: false
  }),
  getters: {
    byId: (s) => (id) => s.boards.find(b => b.id === id)
  },
  actions: {
    async load () {
      try {
        this.boards = (await api.get('/boards')).data
        this.loaded = true
      } catch (e) { notifyError(e) }
    },
    async create (name, color) {
      try {
        const { data } = await api.post('/boards', { name, color })
        this.boards.push(data)
        return data
      } catch (e) { notifyError(e) }
    },
    // patch 可含 name / color / last_view_mode，只送要改的欄位
    async update (id, patch) {
      try {
        const { data } = await api.put(`/boards/${id}`, patch)
        this.boards.splice(this.boards.findIndex(b => b.id === id), 1, data)
        return data
      } catch (e) { notifyError(e) }
    },
    async remove (id) {
      try {
        await api.delete(`/boards/${id}`)
        this.boards = this.boards.filter(b => b.id !== id)
      } catch (e) { notifyError(e) }
    },
    // dir = -1 上移、+1 下移
    async move (id, dir) {
      const ids = this.boards.map(b => b.id)
      const i = ids.indexOf(id)
      const j = i + dir
      if (j < 0 || j >= ids.length) return
      ;[ids[i], ids[j]] = [ids[j], ids[i]]
      try {
        this.boards = (await api.put('/boards/reorder', ids)).data
      } catch (e) { notifyError(e) }
    },
    // columns 為完整陣列，順序即顯示順序；新欄位 id 留空
    async saveColumns (id, columns) {
      try {
        const { data } = await api.put(`/boards/${id}/columns`, columns)
        this.boards.splice(this.boards.findIndex(b => b.id === id), 1, data)
        return data
      } catch (e) { notifyError(e) }
    }
  }
})
