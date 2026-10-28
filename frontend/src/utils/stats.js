// 統計頁的計算（需求書第 4 節）。全部用前端已載入的任務 / 時段 / 分類算，不另開 API。
import { addDays, minutesOf, weekStart } from './timeline.js'
import { UNCATEGORIZED_COLOR } from './task.js'

const WEEKDAYS = '日一二三四五六'
const DAY_BUCKET_MAX = 31 // 超過這麼多天改成每週一根長條

// 'YYYY-MM-DD' → 本地 00:00（new Date('YYYY-MM-DD') 會當成 UTC，在部分時區差一天）
export function parseDate (s) {
  const [y, m, d] = s.split('-').map(Number)
  return new Date(y, m - 1, d)
}

// Date → 'YYYY-MM-DD'（日期輸入框用）
export const toDateInput = d => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

// 期間 [from, to)。kind = 'week' | 'month' | 'custom'；custom 的 to 為包含的最後一天
export function periodRange (kind, now = new Date(), custom = {}) {
  if (kind === 'month') {
    const from = new Date(now.getFullYear(), now.getMonth(), 1)
    return { from, to: new Date(now.getFullYear(), now.getMonth() + 1, 1) }
  }
  if (kind === 'custom' && custom.from && custom.to) {
    return { from: parseDate(custom.from), to: addDays(parseDate(custom.to), 1) }
  }
  const from = weekStart(now)
  return { from, to: addDays(from, 7) }
}

export const dayCount = (from, to) => Math.round((to - from) / 86400000)

export function inRange (blocks, from, to) {
  return blocks.filter(b => { const s = new Date(b.start_at); return s >= from && s < to })
}

export const totalMinutes = blocks => blocks.reduce((n, b) => n + minutesOf(b), 0)

// 有排工時的天數（平均每天工時用這個除，不含沒排的日子）
export const workedDays = blocks => new Set(blocks.map(b => new Date(b.start_at).toDateString())).size

const md = d => `${d.getMonth() + 1}/${String(d.getDate()).padStart(2, '0')}`

// 長條圖的每一根：天數不多時每天一根，否則每週一根
export function buckets (blocks, from, to) {
  const perWeek = dayCount(from, to) > DAY_BUCKET_MAX
  const out = []
  for (let d = perWeek ? weekStart(from) : new Date(from); d < to; d = addDays(d, perWeek ? 7 : 1)) {
    const start = d < from ? from : d
    const end = perWeek ? (addDays(d, 7) > to ? to : addDays(d, 7)) : addDays(d, 1)
    out.push({
      from: start,
      label: perWeek ? md(start) : (dayCount(from, to) <= 7 ? WEEKDAYS[start.getDay()] : String(start.getDate())),
      title: perWeek ? `${md(start)}–${md(addDays(end, -1))}` : `${md(start)}（${WEEKDAYS[start.getDay()]}）`,
      minutes: totalMinutes(inRange(blocks, start, end))
    })
  }
  return { perWeek, list: out }
}

// 各分類工時，多的在前；沒分類或分類已刪除的算「未分類」
export function byCategory (blocks, tasks, categories) {
  const taskCat = Object.fromEntries(tasks.map(t => [t.id, t.category_id]))
  const minutes = {}
  for (const b of blocks) {
    const id = categories.some(c => c.id === taskCat[b.task_id]) ? taskCat[b.task_id] : null
    minutes[id] = (minutes[id] || 0) + minutesOf(b)
  }
  return [...categories.map(c => ({ id: c.id, name: c.name, color: c.color })), { id: null, name: '未分類', color: UNCATEGORIZED_COLOR }]
    .map(c => ({ ...c, minutes: minutes[c.id] || 0 }))
    .filter(c => c.minutes > 0)
    .sort((a, b) => b.minutes - a.minutes)
}

// 圓餅圖：conic-gradient，扇形之間留一條細縫（底色）
export function donutGradient (rows, gapColor, gapPct = 0.6) {
  const total = rows.reduce((n, r) => n + r.minutes, 0)
  if (!total) return 'none'
  let at = 0
  const parts = rows.map(r => {
    const start = at
    at += r.minutes / total * 100
    const end = rows.length > 1 ? Math.max(start, at - gapPct) : at
    return `${r.color} ${start}% ${end}%, ${gapColor} ${end}% ${at}%`
  })
  return `conic-gradient(${parts.join(', ')})`
}

// 完成狀況：未完成 / 已完成為目前全部任務，逾期為未完成且已過死線，本期完成看 completed_at
export function taskCounts (tasks, from, to, now = new Date()) {
  const open = tasks.filter(t => !t.done)
  return {
    open: open.length,
    done: tasks.length - open.length,
    overdue: open.filter(t => t.deadline && new Date(t.deadline) < now).length,
    doneInPeriod: tasks.filter(t => t.done && t.completed_at && new Date(t.completed_at) >= from && new Date(t.completed_at) < to).length
  }
}
