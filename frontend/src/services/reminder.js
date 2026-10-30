import { ref, watch } from 'vue'
import api from './api'
import { dueBlocks, reminderTitle } from '../utils/reminder'

// 工作開始前提醒（Windows 通知）：網頁開著才會提醒，設定記在這台電腦的瀏覽器
const KEY = 'reminder'
const SENT_KEY = 'remindedBlocks'
const CHECK_MS = 30 * 1000
const REFRESH_MS = 5 * 60 * 1000

export const ICON = `${import.meta.env.BASE_URL}icon-192.png`
export const supported = typeof window !== 'undefined' && 'Notification' in window

function loadSettings () {
  try { return { enabled: false, lead: 5, ...JSON.parse(localStorage.getItem(KEY)) } } catch { return { enabled: false, lead: 5 } }
}
export const reminder = ref(loadSettings())
watch(reminder, v => { try { localStorage.setItem(KEY, JSON.stringify(v)) } catch { /* 不記也能用 */ } }, { deep: true })

export const permission = ref(supported ? Notification.permission : 'unsupported')
export async function askPermission () {
  if (supported) permission.value = await Notification.requestPermission()
  return permission.value
}

// 已提醒過的時段（含開始時間，改了時間會再提醒一次）；多個分頁共用，留一天
function loadSent () {
  try { return JSON.parse(localStorage.getItem(SENT_KEY)) || {} } catch { return {} }
}
function markSent (key) {
  const now = Date.now()
  const sent = Object.fromEntries(Object.entries(loadSent()).filter(([, t]) => now - t < 86400000))
  sent[key] = now
  try { localStorage.setItem(SENT_KEY, JSON.stringify(sent)) } catch { /* 頂多重複提醒 */ }
}

let data = { blocks: [], tasks: [], projects: [] }

async function refresh () {
  try {
    const [b, t, p] = await Promise.all(['/blocks', '/tasks', '/projects'].map(u => api.get(u, { silent: true })))
    data = { blocks: b.data, tasks: t.data, projects: p.data }
  } catch { /* 下次再試 */ }
}

function check (onOpen) {
  if (!reminder.value.enabled || !supported || Notification.permission !== 'granted') return
  const sent = loadSent()
  for (const b of dueBlocks(data.blocks, new Date(), reminder.value.lead)) {
    const key = `${b.id}|${b.start_at}`
    const task = data.tasks.find(t => t.id === b.task_id)
    if (sent[key] || !task || task.done) continue
    markSent(key)
    const project = data.projects.find(p => p.id === task.project_id)
    const fmt = s => new Date(s).toLocaleTimeString('zh-TW', { hour: '2-digit', minute: '2-digit', hour12: false })
    // tag 相同時多個分頁同時跳也只會留一則
    const n = new Notification(reminderTitle(task.title, project?.name), { body: `${fmt(b.start_at)} – ${fmt(b.end_at)}`, icon: ICON, tag: key })
    n.onclick = () => { window.focus(); onOpen(); n.close() }
  }
}

/** 開始提醒；回傳停止用的函式。changed：本機改了時段時重新讀取 */
export function startReminder (onOpen, changed) {
  refresh().then(() => check(onOpen))
  const timers = [setInterval(() => check(onOpen), CHECK_MS), setInterval(refresh, REFRESH_MS)]
  const stopWatch = watch(changed, refresh, { deep: true })
  const onFocus = () => refresh()
  window.addEventListener('focus', onFocus)
  return () => { timers.forEach(clearInterval); stopWatch(); window.removeEventListener('focus', onFocus) }
}
