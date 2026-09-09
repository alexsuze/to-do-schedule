// 卡片 / 列表 / 時程表共用的顯示與篩選規則（需求書 3.3.2、3.3.3、3.4.4）

export const PRIORITIES = [
  { label: '高', value: 'high', color: 'negative' },
  { label: '中', value: 'medium', color: 'warning' },
  { label: '低', value: 'low', color: 'primary' }
]

export const DUE_BUCKETS = [
  { label: '逾期', value: 'overdue' },
  { label: '今天', value: 'today' },
  { label: '本週', value: 'week' },
  { label: '之後', value: 'later' },
  { label: '無日期', value: 'none' }
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

// 到期分組：overdue / today / week（到本週日）/ later / none
export function dueBucket (task) {
  if (!task.due_at) return 'none'
  const d = new Date(task.due_at)
  const now = new Date()
  if (d < now) return 'overdue'
  const endToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59, 999)
  if (d <= endToday) return 'today'
  const endWeek = new Date(endToday)
  endWeek.setDate(endWeek.getDate() + (7 - endWeek.getDay()) % 7)
  if (d <= endWeek) return 'week'
  return 'later'
}

export function filterTasks (tasks, f) {
  const q = (f.search || '').trim().toLowerCase()
  return tasks.filter(t =>
    (!f.priority || t.priority === f.priority) &&
    (!f.due || dueBucket(t) === f.due) &&
    (!f.tags?.length || f.tags.every(tag => (t.tags || []).includes(tag))) &&
    (!q || t.title.toLowerCase().includes(q) || (t.description || '').toLowerCase().includes(q))
  )
}

// ISO UTC <-> 本地 'YYYY-MM-DD HH:mm'（DateTimeField 與列表顯示用）
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
