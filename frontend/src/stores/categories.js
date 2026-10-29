import { defineStore } from 'pinia'
import { Notify } from 'quasar'
import api from '../services/api'
import { UNCATEGORIZED_COLOR } from '../utils/task'

const saved = message => Notify.create({ type: 'positive', message })

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
    async create (name, color, projectId) {
      const { data } = await api.post('/categories', { name, color, project_id: projectId })
      this.list.push(data)
      saved(`已新增分類「${data.name}」`)
    },
    // patch 可含 name / color / project_id（換專案＝移動分類，任務跟著移，呼叫端需重新載入任務）
    async update (c, patch) {
      const { data } = await api.put(`/categories/${c.id}`, { name: c.name, color: c.color, project_id: c.project_id, ...patch })
      this.list.splice(this.list.findIndex(x => x.id === c.id), 1, data)
      saved(patch.project_id && patch.project_id !== c.project_id ? `已將「${data.name}」移到其他專案` : `已更新分類「${data.name}」`)
    },
    // 後端會把該分類的任務改成未分類，呼叫端需重新載入任務
    async remove (id) {
      const name = this.byId(id)?.name
      await api.delete(`/categories/${id}`)
      this.list = this.list.filter(c => c.id !== id)
      saved(`已刪除分類「${name}」`)
    },
    async reorder (ids) {
      this.list = (await api.put('/categories/reorder', { ids })).data
      saved('已更新分類順序')
    }
  }
})
