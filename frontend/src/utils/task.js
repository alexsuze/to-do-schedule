// 卡片 / 列表 / 時程表共用的顯示與篩選規則（需求書 3.3.2、3.3.3、3.4.4）

export const PRIORITIES = [
  { label: '高', value: 'high', color: 'negative' },
  { label: '中', value: 'medium', color: 'warning' },
  { label: '低', value: 'low', color: 'primary' }
]

export const TYPES = [
  { label: '任務', value: 'task' },
  { label: '會議', value: 'meeting' }
]

export const DUE_BUCKETS = [
  { label: '逾期', value: 'overdue' },
  { label: '已結束', value: 'ended' },
  { label: '今天', value: 'today' },
  { label: '本週', value: 'week' },
  { label: '之後', value: 'later' },
  { label: '無日期', value: 'none' }
]

export const isMeeting = t => t.type === 'meeting'

// 提醒與顯示的基準時間：會議看開始，任務看到期
export const anchorAt = t => isMeeting(t) ? t.start_at : t.due_at

export function priorityColor (p) {
  return PRIORITIES.find(x => x.value === p)?.color || 'grey'
}

const pad = n => String(n).padStart(2, '0')

// 底色深時文字用白色
export function textOn (hex) {
  if (!hex) return '#000'
  const n = parseInt(hex.slice(1), 16)
  const lum = (0.299 * (n >> 16) + 0.587 * ((n >> 8) & 255) + 0.114 * (n & 255)) / 255
  return lum < 0.6 ? '#fff' : '#000'
}

// ponytail: 以瀏覽器時區顯示，設定檔時區留到階段 5
export function formatDue (iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return `${d.getMonth() + 1}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 回傳 'done' | 'overdue' | 'ended' | 'soon' | ''
// 任務：到期前 24 小時 soon、過期 overdue；會議：開始前 24 小時 soon、結束（無結束則開始）後 ended
export function dueState (task) {
  if (task.completed_at) return 'done'
  const at = anchorAt(task)
  if (!at) return ''
  const now = Date.now()
  if (isMeeting(task) && new Date(task.due_at || at) < now) return 'ended'
  const diff = new Date(at) - now
  if (diff < 0) return 'overdue'
  if (diff < 24 * 3600 * 1000) return 'soon'
  return ''
}

export function stateLabel (task) {
  const s = dueState(task)
  if (s === 'ended') return '已結束'
  if (s === 'soon' && isMeeting(task)) return '即將開始'
  return ''
}

// 到期分組：overdue / ended / today / week（到本週日）/ later / none（會議以開始時間分組）
export function dueBucket (task) {
  const at = anchorAt(task)
  if (!at) return 'none'
  if (dueState(task) === 'ended') return 'ended'
  const d = new Date(at)
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
