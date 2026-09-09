import { defineStore } from 'pinia'
import { Notify } from 'quasar'
import api from '../services/api'

function notifyError (e) {
  Notify.create({ type: 'negative', message: e.response?.data?.detail || e.message })
  throw e
}

// 只保存「目前看板」的事項；切換看板時重新載入
export const useTasksStore = defineStore('tasks', {
  state: () => ({
    boardId: null,
    tasks: []
  }),
  getters: {
    byColumn: (s) => (columnId) =>
      s.tasks.filter(t => t.column_id === columnId).sort((a, b) => a.order - b.order),
    byId: (s) => (id) => s.tasks.find(t => t.id === id)
  },
  actions: {
    async load (boardId) {
      this.boardId = boardId
      try {
        this.tasks = (await api.get(`/boards/${boardId}/tasks`)).data
      } catch (e) { notifyError(e) }
    },
    async create (title) {
      try {
        const { data } = await api.post(`/boards/${this.boardId}/tasks`, { title })
        this.tasks.push(data)
        return data
      } catch (e) { notifyError(e) }
    },
    // fields：title, description, priority, start_at, due_at, tags（完整覆寫）
    async update (id, fields) {
      try {
        const { data } = await api.put(`/tasks/${id}`, fields)
        this.replace(data)
        return data
      } catch (e) { notifyError(e) }
    },
    // 行內修改單一欄位：後端 update 為完整覆寫，故帶上原值
    patch (task, changes) {
      return this.update(task.id, {
        title: task.title,
        description: task.description,
        priority: task.priority,
        start_at: task.start_at,
        due_at: task.due_at,
        tags: task.tags,
        ...changes
      })
    },
    // 移動後其他卡片的 order 也會變，直接重新載入最省事
    async move (id, { boardId = null, columnId, index = null }) {
      try {
        await api.put(`/tasks/${id}/move`, { board_id: boardId, column_id: columnId, index })
      } catch (e) {
        await this.load(this.boardId)
        notifyError(e)
      }
      await this.load(this.boardId)
    },
    async remove (id) {
      try {
        await api.delete(`/tasks/${id}`)
        this.tasks = this.tasks.filter(t => t.id !== id)
      } catch (e) { notifyError(e) }
    },
    replace (task) {
      const i = this.tasks.findIndex(t => t.id === task.id)
      if (i >= 0) this.tasks.splice(i, 1, task)
    }
  }
})
