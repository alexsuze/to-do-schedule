import { defineStore } from 'pinia'
import { Notify } from 'quasar'
import api from '../services/api'
import { ALL_COMPANIES, resolveCompany } from '../utils/company'

const saved = message => Notify.create({ type: 'positive', message })

// 這台電腦記住上次選的公司（需求書 12.2）；同一個 github.io 網域下要加專案前綴
const KEY = 'to-do-schedule:company'
function read () {
  try { return JSON.parse(localStorage.getItem(KEY) || '{}') } catch { return {} }
}
function write (v) {
  try { localStorage.setItem(KEY, JSON.stringify(v)) } catch { /* 不記也能用 */ }
}

// 公司（需求書第 12 節）：公司 → 專案 → 分類 → 任務
export const useCompaniesStore = defineStore('companies', {
  state: () => {
    const r = read()
    return {
      list: [],
      loaded: false,
      // 最後一次選的單一公司（時程表、統計、設定、封存、回收桶用）
      singleId: r.single || null,
      // 任務頁是否選了「全部公司」
      all: r.all === true
    }
  },
  getters: {
    byId: (s) => (id) => s.list.find(c => c.id === id),
    active: (s) => s.list.filter(c => !c.archived),
    // 目前的單一公司（物件）；還沒載入時為 undefined
    current: (s) => s.list.find(c => c.id === s.singleId),
    currentId: (s) => s.singleId,
    // 任務頁用：'all' 或公司 id
    taskScope: (s) => s.all ? ALL_COMPANIES : s.singleId,
    label: (s) => (id) => {
      const c = s.list.find(x => x.id === id)
      return c ? { name: c.name, color: c.color, archived: c.archived } : { name: '未指定公司', color: '#D9D4E0', archived: false }
    }
  },
  actions: {
    async load () {
      this.list = (await api.get('/companies')).data
      this.loaded = true
      this.fix()
    },
    // 記住的公司被封存或不存在時，改用排序第一家未封存的
    fix () {
      const id = resolveCompany(this.list, this.singleId)
      if (id !== this.singleId) this.singleId = id
      this.persist()
    },
    persist () {
      write({ single: this.singleId, all: this.all })
    },
    // id 為公司 id 或 'all'（只有任務頁可選）
    select (id) {
      if (id === ALL_COMPANIES) this.all = true
      else {
        this.all = false
        this.singleId = id
      }
      this.persist()
    },
    async create (name, color) {
      const { data } = await api.post('/companies', { name, color })
      this.list.push(data)
      saved(`已新增公司「${data.name}」`)
      return data
    },
    // patch 可含 name / color / archived
    async update (c, patch) {
      const { data } = await api.put(`/companies/${c.id}`, { name: c.name, color: c.color, ...patch })
      this.list.splice(this.list.findIndex(x => x.id === c.id), 1, data)
      this.fix()
      saved(patch.archived === true ? `已封存「${data.name}」` : patch.archived === false ? `已取消封存「${data.name}」` : `已更新公司「${data.name}」`)
    },
    async reorder (ids) {
      this.list = (await api.put('/companies/reorder', { ids })).data
      this.fix()
      saved('已更新公司順序')
    }
  }
})
