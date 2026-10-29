import { defineStore } from 'pinia'
import { Notify } from 'quasar'
import api from '../services/api'
import { NO_PROJECT_COLOR } from '../utils/task'

const saved = message => Notify.create({ type: 'positive', message })

// 專案（需求書第 10 節）：專案 → 分類 → 任務
export const useProjectsStore = defineStore('projects', {
  state: () => ({
    list: []
  }),
  getters: {
    byId: (s) => (id) => s.list.find(p => p.id === id),
    // 未封存的專案（新增 / 編輯任務、篩選用）
    active: (s) => s.list.filter(p => !p.archived),
    // 顯示用：找不到就是無專案
    label: (s) => (id) => {
      const p = s.list.find(x => x.id === id)
      return p ? { name: p.name, color: p.color, archived: p.archived } : { name: '無專案', color: NO_PROJECT_COLOR, archived: false }
    }
  },
  actions: {
    async load () {
      this.list = (await api.get('/projects')).data
    },
    async create (name, color) {
      const { data } = await api.post('/projects', { name, color })
      this.list.push(data)
      saved(`已新增專案「${data.name}」`)
      return data
    },
    // patch 可含 name / color / archived
    async update (p, patch) {
      const { data } = await api.put(`/projects/${p.id}`, { name: p.name, color: p.color, ...patch })
      this.list.splice(this.list.findIndex(x => x.id === p.id), 1, data)
      saved(patch.archived === true ? `已封存「${data.name}」` : patch.archived === false ? `已取消封存「${data.name}」` : `已更新專案「${data.name}」`)
    },
    // 後端會刪掉專案的分類、把任務改成無專案；呼叫端需重新載入分類與任務
    async remove (id) {
      const name = this.byId(id)?.name
      await api.delete(`/projects/${id}`)
      this.list = this.list.filter(p => p.id !== id)
      saved(`已刪除專案「${name}」`)
    },
    async reorder (ids) {
      this.list = (await api.put('/projects/reorder', { ids })).data
      saved('已更新專案順序')
    }
  }
})
