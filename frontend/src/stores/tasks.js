import { defineStore } from 'pinia'
import api from '../services/api'

// 後端 TaskRequest 的欄位（編輯為完整覆寫）
const toRequest = t => ({
  title: t.title,
  description: t.description,
  category_id: t.category_id || null,
  deadline: t.deadline || null,
  subtasks: t.subtasks || [],
  done: !!t.done
})

export const useTasksStore = defineStore('tasks', {
  state: () => ({
    tasks: [],
    trash: []
  }),
  actions: {
    async load () {
      this.tasks = (await api.get('/tasks')).data
    },
    // 有 id 為編輯，否則新增
    async save (task) {
      const { data } = task.id
        ? await api.put(`/tasks/${task.id}`, toRequest(task))
        : await api.post('/tasks', toRequest(task))
      const i = this.tasks.findIndex(t => t.id === data.id)
      if (i >= 0) this.tasks.splice(i, 1, data)
      else this.tasks.push(data)
      return data
    },
    setDone (task, done) {
      return this.save({ ...task, done })
    },
    async remove (id) {
      await api.delete(`/tasks/${id}`)
      this.tasks = this.tasks.filter(t => t.id !== id)
    },

    /* 回收桶 */
    async loadTrash () {
      this.trash = (await api.get('/trash')).data
    },
    async restore (id) {
      const { data } = await api.post(`/trash/${id}/restore`)
      this.trash = this.trash.filter(t => t.id !== id)
      this.tasks.push(data)
    },
    async purge (id) {
      await api.delete(`/trash/${id}`)
      this.trash = this.trash.filter(t => t.id !== id)
    },
    async purgeAll () {
      await api.delete('/trash')
      this.trash = []
    }
  }
})
