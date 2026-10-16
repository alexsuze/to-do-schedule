// 執行：npm test（Node 內建測試，不需額外套件）
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { dueBucket, dueState, filterTasks, isoToLocal, localToIso, nestStages, showOnKanban, stageInfo } from './task.js'

const hours = h => new Date(Date.now() + h * 3600000).toISOString()

test('dueState', () => {
  assert.equal(dueState({ completed_at: 'x', due_at: hours(-1) }), 'done')
  assert.equal(dueState({ due_at: hours(-1) }), 'overdue')
  assert.equal(dueState({ due_at: hours(2) }), 'soon')
  assert.equal(dueState({ due_at: hours(48) }), '')
  assert.equal(dueState({}), '')
  // 會議看開始時間，結束後為 ended 而非 overdue
  assert.equal(dueState({ type: 'meeting', start_at: hours(2), due_at: hours(3) }), 'soon')
  assert.equal(dueState({ type: 'meeting', start_at: hours(-2), due_at: hours(-1) }), 'ended')
  assert.equal(dueState({ type: 'meeting', start_at: hours(-1) }), 'ended')
  assert.equal(dueState({ type: 'meeting', start_at: hours(48) }), '')
  assert.equal(dueBucket({ type: 'meeting', start_at: hours(-1) }), 'ended')
})

test('dueBucket', () => {
  assert.equal(dueBucket({}), 'none')
  assert.equal(dueBucket({ due_at: hours(-1) }), 'overdue')
  assert.equal(dueBucket({ due_at: hours(24 * 30) }), 'later')
  const endToday = new Date(); endToday.setHours(23, 59, 0, 0)
  assert.equal(dueBucket({ due_at: endToday.toISOString() }), 'today')
})

test('filterTasks', () => {
  const tasks = [
    { title: 'Alpha', description: '', priority: 'high', tags: ['a', 'b'] },
    { title: 'Beta', description: 'has alpha inside', priority: 'low', tags: ['b'], due_at: hours(-1) }
  ]
  const f = (o) => ({ priority: null, due: null, tags: [], search: '', ...o })
  assert.equal(filterTasks(tasks, f({})).length, 2)
  assert.equal(filterTasks(tasks, f({ priority: 'high' }))[0].title, 'Alpha')
  assert.equal(filterTasks(tasks, f({ search: 'ALPHA' })).length, 2)
  assert.equal(filterTasks(tasks, f({ tags: ['a', 'b'] })).length, 1)
  assert.equal(filterTasks(tasks, f({ tags: ['b'] })).length, 2)
  assert.equal(filterTasks(tasks, f({ due: 'overdue' }))[0].title, 'Beta')
  assert.equal(filterTasks(tasks, f({ due: 'none' }))[0].title, 'Alpha')
})

test('stages', () => {
  const tasks = [
    { id: 'p', title: 'P' },
    { id: 's2', parent_id: 'p', stage_order: 1 },
    { id: 's1', parent_id: 'p', stage_order: 0, completed_at: 'x' },
    { id: 'q', title: 'Q' }
  ]
  const info = stageInfo(tasks, 'p')
  assert.deepEqual(info.stages.map(s => s.id), ['s1', 's2'])
  assert.equal(info.done, 1)
  assert.equal(info.current.id, 's2')
  assert.equal(showOnKanban(tasks, tasks[1]), true)
  assert.equal(showOnKanban(tasks, tasks[2]), false)
  assert.equal(showOnKanban(tasks, tasks[3]), true)
  assert.deepEqual(nestStages(tasks).map(t => t.id), ['p', 's1', 's2', 'q'])
  // 所屬任務被篩掉時，階段仍保留
  assert.deepEqual(nestStages(tasks.slice(1)).map(t => t.id), ['s2', 's1', 'q'])
})

test('local <-> iso round trip', () => {
  const iso = localToIso('2026-09-10 09:30')
  assert.equal(isoToLocal(iso), '2026-09-10 09:30')
  assert.equal(localToIso(''), null)
  assert.equal(localToIso('2026-09'), null)
  assert.equal(isoToLocal(null), '')
})
