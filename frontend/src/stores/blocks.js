import { defineStore } from 'pinia'
import api from '../services/api'
import { minutesByTask } from '../utils/timeline'

// 時程表時段；後端只回傳所屬任務不在回收桶的時段
export const useBlocksStore = defineStore('blocks', {
  state: () => ({
    list: []
  }),
  getters: {
    minutesByTask: (s) => minutesByTask(s.list),
    countOf: (s) => (taskId) => s.list.filter(b => b.task_id === taskId).length
  },
  actions: {
    async load () {
      this.list = (await api.get('/blocks')).data
    },
    async create (taskId, start, end) {
      const { data } = await api.post('/blocks', { task_id: taskId, start_at: start.toISOString(), end_at: end.toISOString() })
      this.list.push(data)
    },
    async update (b, start, end) {
      const { data } = await api.put(`/blocks/${b.id}`, { start_at: start.toISOString(), end_at: end.toISOString() })
      this.list.splice(this.list.findIndex(x => x.id === b.id), 1, data)
    },
    async remove (id) {
      await api.delete(`/blocks/${id}`)
      this.list = this.list.filter(b => b.id !== id)
    }
  }
})
