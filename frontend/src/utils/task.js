// 卡片 / 列表共用的顯示規則（需求書 3.3.2）

export const PRIORITIES = [
  { label: '高', value: 'high', color: 'negative' },
  { label: '中', value: 'medium', color: 'warning' },
  { label: '低', value: 'low', color: 'primary' }
]

export function priorityColor (p) {
  return PRIORITIES.find(x => x.value === p)?.color || 'grey'
}

// ponytail: 以瀏覽器時區顯示，設定檔時區留到階段 5
export function formatDue (iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = n => String(n).padStart(2, '0')
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

// datetime-local 的值（本地時間，無時區）<-> ISO UTC
export function isoToLocalInput (iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function localInputToIso (v) {
  return v ? new Date(v).toISOString() : null
}
