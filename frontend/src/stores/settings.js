import { defineStore } from 'pinia'
import { Notify } from 'quasar'
import api from '../services/api'

function notifyError (e) {
  Notify.create({ type: 'negative', message: e.response?.data?.detail || e.message })
  throw e
}

export const LEAD_TIME_OPTIONS = [
  { label: '到期時', value: '0m' },
  { label: '15 分鐘前', value: '15m' },
  { label: '1 小時前', value: '1h' },
  { label: '1 天前', value: '1d' },
  { label: '3 天前', value: '3d' },
  { label: '1 週前', value: '1w' }
]

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    settings: null,
    notifications: []
  }),
  actions: {
    async load () {
      try {
        this.settings = (await api.get('/settings')).data
      } catch (e) { notifyError(e) }
    },
    // patch 只帶要改的欄位
    async save (patch) {
      try {
        this.settings = (await api.put('/settings', patch)).data
        return this.settings
      } catch (e) { notifyError(e) }
    },
    async sendTest () {
      try {
        const { data } = await api.post('/settings/test-email')
        Notify.create({ type: 'positive', message: `測試信已寄出（${data.message_id}）` })
      } catch (e) { notifyError(e) }
    },
    async loadNotifications () {
      try {
        this.notifications = (await api.get('/notifications')).data
      } catch (e) { notifyError(e) }
    }
  }
})
