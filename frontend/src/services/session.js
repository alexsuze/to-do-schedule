// 登入通行證與使用者資料存在瀏覽器（重新整理後仍保持登入）。讀寫失敗（無痕模式等）就當作沒登入
const KEY = 'auth'

export function loadSession () {
  try { return JSON.parse(localStorage.getItem(KEY)) || {} } catch { return {} }
}

export const authToken = () => loadSession().token || null

export function saveSession (token, user) {
  try { localStorage.setItem(KEY, JSON.stringify({ token, user })) } catch { /* 這次有效，關掉就要重登 */ }
}

export function clearSession () {
  try { localStorage.removeItem(KEY) } catch { /* 忽略 */ }
}
