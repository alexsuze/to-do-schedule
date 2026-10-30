// 工作開始前提醒：哪些時段該提醒（純函式，方便測試）

/**
 * 回傳「leadMin 分鐘內就要開始、還沒開始」的時段。
 * 同一任務接續上一格（上一格結束 = 這格開始）的不算新開始，不提醒。
 */
export function dueBlocks (blocks, now, leadMin) {
  const t = now.getTime()
  const ends = new Set(blocks.map(b => `${b.task_id}|${new Date(b.end_at).getTime()}`))
  return blocks.filter(b => {
    const s = new Date(b.start_at).getTime()
    return s > t && s - t <= leadMin * 60000 && !ends.has(`${b.task_id}|${s}`)
  })
}

/** 通知標題：【專案名】任務名 即將開始 */
export function reminderTitle (taskTitle, projectName) {
  return `${projectName ? `【${projectName}】` : ''}${taskTitle} 即將開始`
}
