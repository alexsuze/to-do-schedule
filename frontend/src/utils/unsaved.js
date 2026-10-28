import { onMounted, onUnmounted } from 'vue'
import { Dialog } from 'quasar'
import { onBeforeRouteLeave } from 'vue-router'
import UnsavedDialog from '../components/UnsavedDialog.vue'

// 未儲存防呆，所有「按儲存才生效」的地方共用

// 回傳 'save' | 'discard' | 'stay'
export function askUnsaved (message) {
  return new Promise(resolve => {
    Dialog.create({ component: UnsavedDialog, componentProps: message ? { message } : {} })
      .onOk(resolve)
      .onCancel(() => resolve('stay'))
  })
}

// 問完照選擇處理；回傳 true 表示可以離開。save() 失敗（回傳 false 或丟錯）就留下
export async function resolveUnsaved (save, message) {
  const choice = await askUnsaved(message)
  if (choice === 'stay') return false
  if (choice === 'discard') return true
  try {
    return (await save()) !== false
  } catch {
    return false
  }
}

// 換頁（含瀏覽器上一頁）時跳提示；重新整理 / 關閉分頁時由瀏覽器跳出它自己的確認（無法自訂按鈕）
export function useUnsavedGuard (isDirty, save, message) {
  const onBeforeUnload = e => {
    if (!isDirty()) return
    e.preventDefault()
    e.returnValue = ''
  }
  onMounted(() => window.addEventListener('beforeunload', onBeforeUnload))
  onUnmounted(() => window.removeEventListener('beforeunload', onBeforeUnload))
  onBeforeRouteLeave(() => !isDirty() || resolveUnsaved(save, typeof message === 'function' ? message() : message))
}
