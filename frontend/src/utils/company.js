// 公司相關的純函式（需求書第 12 節）

export const ALL_COMPANIES = 'all'

// 記住的公司 id 若不存在或已封存，改用排序第一家未封存的公司；一家都沒有回傳 null
export function resolveCompany (list, id) {
  const ok = list.find(c => c.id === id && !c.archived)
  if (ok) return ok.id
  const first = list.find(c => !c.archived) || list[0]
  return first ? first.id : null
}

// 公司篩選：scope 為 'all' 或公司 id
export const inCompany = (item, scope) => scope === ALL_COMPANIES || item.company_id === scope

// 時段所屬公司＝任務的公司；taskCompany 為 { taskId: companyId }
export function splitBlocks (blocks, taskCompany, companyId) {
  const mine = []
  const others = []
  for (const b of blocks) (taskCompany[b.task_id] === companyId ? mine : others).push(b)
  return { mine, others }
}

// 自己的時段是否和其他公司的時段重疊（時程表右邊留空隙露出斜線）
export const overlapsAny = (b, others) =>
  others.some(o => new Date(o.start_at) < new Date(b.end_at) && new Date(b.start_at) < new Date(o.end_at))

// 公司方塊上的字：名稱第一個字（英文取大寫）
export const initialOf = name => (name || '?').trim().slice(0, 1).toUpperCase()
