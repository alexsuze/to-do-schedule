import { defineStore } from 'pinia'
import api from '../services/api'
import { useBlocksStore } from './blocks'
import { DEFAULT_ARCHIVE_DAYS } from '../utils/task'

// 後端 TaskRequest 的欄位（編輯為完整覆寫）
const toRequest = t => ({
  title: t.title,
  description: t.description,
  company_id: t.company_id || null,
  project_id: t.project_id || null,
  category_id: t.category_id || null,
  start_at: t.start_at || null,
  deadline: t.deadline || null,
  subtasks: t.subtasks || [],
  done: !!t.done
})

export const useTasksStore = defineStore('tasks', {
  state: () => ({
    tasks: [],
    trash: [],
    archiveDays: DEFAULT_ARCHIVE_DAYS // 已完成幾天後自動封存（個人設定）
  }),
  actions: {
    async load () {
      this.tasks = (await api.get('/tasks')).data
    },
    async loadArchiveDays () {
      this.archiveDays = (await api.get('/settings')).data.archive_days
    },
    async setArchiveDays (days) {
      this.archiveDays = (await api.put('/settings/archive-days', { days })).data.archive_days
    },
    // 有 id 為編輯，否則新增
    async save (task) {
      const { data } = task.id
        ? await api.put(`/tasks/${task.id}`, toRequest(task))
        : await api.post('/tasks', toRequest(task))
      return this.put(data)
    },
    put (data) {
      const i = this.tasks.findIndex(t => t.id === data.id)
      if (i >= 0) this.tasks.splice(i, 1, data)
      else this.tasks.push(data)
      return data
    },
    async archive (id) {
      return this.put((await api.post(`/tasks/${id}/archive`)).data)
    },
    async unarchive (id) {
      return this.put((await api.post(`/tasks/${id}/unarchive`)).data)
    },
    setDone (task, done) {
      return this.save({ ...task, done })
    },
    async remove (id) {
      await api.delete(`/tasks/${id}`)
      this.tasks = this.tasks.filter(t => t.id !== id)
      // 進回收桶的任務，時段暫時從時程表消失
      const blocks = useBlocksStore()
      blocks.list = blocks.list.filter(b => b.task_id !== id)
    },

    /* 回收桶 */
    async loadTrash () {
      this.trash = (await api.get('/trash')).data
    },
    // 回傳因時間被佔用而沒還原的時段數
    async restore (id) {
      const { data } = await api.post(`/trash/${id}/restore`)
      this.trash = this.trash.filter(t => t.id !== id)
      this.tasks.push(data.task)
      await useBlocksStore().load()
      return data.dropped_blocks
    },
    async purge (id) {
      await api.delete(`/trash/${id}`)
      this.trash = this.trash.filter(t => t.id !== id)
    },
    // 只清空這家公司的
    async purgeAll (companyId) {
      await api.delete('/trash', { params: { company_id: companyId } })
      this.trash = this.trash.filter(t => t.company_id !== companyId)
    }
  }
})
