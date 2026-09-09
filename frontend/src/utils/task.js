// 卡片 / 列表共用的顯示規則（需求書 3.3.2）

export const PRIORITIES = [
  { label: '高', value: 'high', color: 'negative' },
  { label: '中', value: 'medium', color: 'warning' },
  { label: '低', value: 'low', color: 'primary' }
]

export function priorityColor (p) {
  return PRIORITIES.find(x => x.value === p)?.color || 'grey'
}

const pad = n => String(n).padStart(2, '0')

// ponytail: 以瀏覽器時區顯示，設定檔時區留到階段 5
export function formatDue (iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return `${d.getMonth() + 1}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 回傳 'done' | 'overdue' | 'soon' | ''
export function dueState (task) {
  if (task.completed_at) return 'done'
  if (!task.due_at) return ''
  const diff = new Date(task.due_at) - Date.now()
  if (diff < 0) return 'overdue'
  if (diff < 24 * 3600 * 1000) return 'soon'
  return ''
}

// ISO UTC <-> 本地 'YYYY-MM-DD HH:mm'（DateTimeField 用）
export function isoToLocal (iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function localToIso (v) {
  if (!v || v.length < 16) return null
  const d = new Date(v.replace(' ', 'T'))
  return isNaN(d) ? null : d.toISOString()
}
