// 任務卡 / 回收桶共用的顯示規則（需求書 2.2、2.4）

const pad = n => String(n).padStart(2, '0')
const DAY = 24 * 3600 * 1000
const WEEKDAYS = '日一二三四五六'

export const TRASH_KEEP_DAYS = 30
export const ARCHIVE_DAY_OPTIONS = [7, 14, 30, 60, 90] // 與後端 SettingsService.ARCHIVE_DAYS 一致
export const DEFAULT_ARCHIVE_DAYS = 14

// 分類預設色盤（粉嫩色，文字一律用深色）；新增分類依序輪流取色
export const PALETTE = [
  '#8FD3FF', '#8EE3B8', '#FFD966', '#FFB3C7', '#C9B6FF', '#FFC49E', '#A6E3E9', '#E2DCEB',
  '#FF9F9F', '#C5E88A', '#7FD8C9', '#A9B8FF', '#F5A3D8', '#FFB570', '#E8D3B0', '#B9D7A8'
]
export const UNCATEGORIZED_COLOR = '#E2DCEB'
export const NO_PROJECT_COLOR = '#D9D4E0' // 無專案：灰色

// 'overdue' | 'soon'（24 小時內）| ''；已完成不提示
export function deadlineState (task, now = Date.now()) {
  if (task.done || !task.deadline) return ''
  const diff = new Date(task.deadline) - now
  if (diff < 0) return 'overdue'
  if (diff < DAY) return 'soon'
  return ''
}

// 10/30（五）10:00
export function formatDateTime (iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return `${d.getMonth() + 1}/${pad(d.getDate())}（${WEEKDAYS[d.getDay()]}）${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 卡片上的死線文字：逾期 / 24 小時內用口語，其餘顯示日期
export function deadlineLabel (task, now = Date.now()) {
  const state = deadlineState(task, now)
  if (state === 'overdue') {
    const days = Math.floor((now - new Date(task.deadline)) / DAY)
    return days < 1 ? '已逾期' : `已逾期 ${days} 天`
  }
  if (state === 'soon') {
    const d = new Date(task.deadline)
    const today = new Date(now).getDate() === d.getDate()
    return `${today ? '今天' : '明天'} ${pad(d.getHours())}:${pad(d.getMinutes())} 到期`
  }
  return formatDateTime(task.deadline)
}

// 複製任務的標題：原標題_複製；標題上限 100 字，超過就截短原標題
export const COPY_SUFFIX = '_複製'
export const copyTitle = title => `${(title || '').slice(0, 100 - COPY_SUFFIX.length)}${COPY_SUFFIX}`

// 開始時間在 end 之前（或沒填＝已開始）；時程表用週末判斷這週要不要列出
export const startedBefore = (task, end) => !task.start_at || new Date(task.start_at) < end

// 時程表左側要不要列出（需求書 3.2）：週 [start, end) 結束前已開始；
// 看往後的週（週一在現在之後）時，死線早於那週週一的不列。本週與過去的週照列，逾期的還能補排
export const listedInWeek = (task, start, end, now = Date.now()) =>
  startedBefore(task, end) && !(start > now && task.deadline && new Date(task.deadline) < start)

// 開始時間還沒到
export const notStarted = (task, now = Date.now()) => !!task.start_at && new Date(task.start_at) > now

// 還沒開始的任務卡標籤，例：11/05（四）09:00 開始；已開始回傳空字串
export const startLabel = (task, now = Date.now()) =>
  notStarted(task, now) ? `${formatDateTime(task.start_at)} 開始` : ''

// 封存（需求書 11）：已完成且（手動封存，或完成 / 解除封存兩者較晚者已滿 days 天）
export function isArchived (task, days, now = Date.now()) {
  if (!task.done) return false
  if (task.archived_at) return true
  const from = Math.max(new Date(task.completed_at || 0), new Date(task.unarchived_at || 0))
  return from > 0 && now - from >= days * DAY
}

// 死線近的在前，沒死線的在後；同死線依建立時間
export function sortTasks (tasks) {
  const key = t => t.deadline ? new Date(t.deadline).getTime() : Infinity
  return [...tasks].sort((a, b) => key(a) - key(b) || String(a.created_at).localeCompare(String(b.created_at)))
}

export function subtaskProgress (task) {
  const list = task.subtasks || []
  return { done: list.filter(s => s.done).length, total: list.length }
}

// 回收桶：還剩幾天會被自動永久刪除（最少 0）
export function trashDaysLeft (deletedAt, now = Date.now()) {
  return Math.max(0, TRASH_KEEP_DAYS - Math.floor((now - new Date(deletedAt)) / DAY))
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

// 專案篩選：'all' 全部、'none' 無專案、其他為專案 id
export function matchProject (task, filter) {
  if (filter === 'all') return true
  if (filter === 'none') return !task.project_id
  return task.project_id === filter
}
