import { Dark } from 'quasar'

// 深色 / 淺色選擇記在瀏覽器（需求書第 1 節）；無痕模式等讀寫失敗時就不記
const KEY = 'theme'

export function savedTheme () {
  try {
    const v = localStorage.getItem(KEY)
    return v === 'dark' ? true : v === 'light' ? false : null
  } catch { return null }
}

export function setDark (on) {
  Dark.set(on)
  try { localStorage.setItem(KEY, on ? 'dark' : 'light') } catch { /* 不記也能用 */ }
}
