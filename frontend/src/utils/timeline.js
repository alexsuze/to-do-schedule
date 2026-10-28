// 時程表的時間計算（需求書第 3 節）。一格 = 30 分鐘，一天 48 格；時間以瀏覽器時區顯示。

export const SLOTS_PER_DAY = 48
export const DEFAULT_RANGE = { start: 9, end: 18 } // 預設顯示 09:00–18:00
export const EXPAND_HOURS = 3

const pad = n => String(n).padStart(2, '0')

// 該週週一 00:00
export function weekStart (d) {
  const x = new Date(d)
  x.setHours(0, 0, 0, 0)
  x.setDate(x.getDate() - (x.getDay() + 6) % 7)
  return x
}

// 用 setDate 加天數，遇到日光節約也不會偏移
export function addDays (d, n) {
  const x = new Date(d)
  x.setDate(x.getDate() + n)
  return x
}

export const sameDay = (a, b) => a.toDateString() === b.toDateString()

// 某天第 slot 格的時間；slot = 48 即隔天 00:00
export function slotTime (day, slot) {
  const x = new Date(day)
  x.setHours(0, slot * 30, 0, 0)
  return x
}

// 時間在當天是第幾格
export const slotOf = d => d.getHours() * 2 + Math.floor(d.getMinutes() / 30)

export const minutesOf = b => (new Date(b.end_at) - new Date(b.start_at)) / 60000

// 時段在當天的起始格與格數
export function blockSlots (b) {
  const start = slotOf(new Date(b.start_at))
  return { start, len: minutesOf(b) / 30 }
}

// 90 → '1.5h'
export function formatHours (minutes) {
  return `${Math.round(minutes / 60 * 10) / 10}h`
}

export const formatSlot = slot => `${pad(Math.floor(slot / 2))}:${slot % 2 ? '30' : '00'}`

export function minutesByTask (blocks) {
  const m = {}
  for (const b of blocks) m[b.task_id] = (m[b.task_id] || 0) + minutesOf(b)
  return m
}

// [start, end) 是否和其他時段重疊；ignoreId 為正在移動的時段自己
export function overlaps (blocks, start, end, ignoreId) {
  return blocks.some(b => b.id !== ignoreId && new Date(b.start_at) < end && new Date(b.end_at) > start)
}

// 顯示範圍（小時）：使用者設定的範圍，再撐開到能看見本週所有時段
export function visibleRange (range, blocks) {
  let { start, end } = range
  for (const b of blocks) {
    const s = blockSlots(b)
    start = Math.min(start, Math.floor(s.start / 2))
    end = Math.max(end, Math.ceil((s.start + s.len) / 2))
  }
  return { start, end }
}
