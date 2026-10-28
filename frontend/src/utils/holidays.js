// 台灣國定假日 / 補班日（時程表標示用）
// 資料來源：社群整理的行政院人事行政總處辦公日曆（每年一份 JSON）
// ponytail: 直接讀第三方公開檔，讀不到就不標示；來源停更時再換成自己維護的清單
const SOURCE = year => `https://cdn.jsdelivr.net/gh/ruyut/TaiwanCalendar/data/${year}.json`
const CACHE_KEY = year => `holidays-${year}`

const pad = n => String(n).padStart(2, '0')
export const dateKey = d => `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}`

// 只留特殊日子：平日放假（國定假日、補假）與週末上班（補班）
// 回傳 { 'YYYYMMDD': { off: true, name } | { off: false, name: '補班' } }
export function toSpecialDays (rows) {
  const out = {}
  for (const r of rows) {
    const weekend = r.week === '六' || r.week === '日'
    if (r.isHoliday && !weekend) out[r.date] = { off: true, name: r.description || '放假' }
    else if (r.isHoliday && r.description) out[r.date] = { off: true, name: r.description } // 週末剛好是國定假日
    else if (!r.isHoliday && weekend) out[r.date] = { off: false, name: '補班' }
  }
  return out
}

const memory = {}

// 取得某年的特殊日子；先看記憶體、再看瀏覽器快取、最後才下載。失敗回傳 {}（不標示）
export async function loadYear (year) {
  if (memory[year]) return memory[year]
  try {
    const cached = localStorage.getItem(CACHE_KEY(year))
    if (cached) return (memory[year] = JSON.parse(cached))
  } catch { /* 快取不能用就直接下載 */ }
  try {
    const res = await fetch(SOURCE(year))
    if (!res.ok) return {}
    const days = toSpecialDays(await res.json())
    memory[year] = days
    try { localStorage.setItem(CACHE_KEY(year), JSON.stringify(days)) } catch { /* 不記也能用 */ }
    return days
  } catch {
    return {}
  }
}
