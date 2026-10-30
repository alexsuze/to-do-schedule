import { test } from 'node:test'
import assert from 'node:assert/strict'
import { startedBefore, startLabel, deadlineState, deadlineLabel, sortTasks, subtaskProgress, trashDaysLeft, isoToLocal, localToIso } from './task.js'

const now = new Date('2026-10-28T10:00:00').getTime()
const at = s => new Date(s).toISOString()

test('deadline state and label', () => {
  assert.equal(deadlineState({ deadline: at('2026-10-27T18:00:00') }, now), 'overdue')
  assert.equal(deadlineLabel({ deadline: at('2026-10-26T18:00:00') }, now), '已逾期 1 天')
  assert.equal(deadlineState({ deadline: at('2026-10-29T09:00:00') }, now), 'soon')
  assert.equal(deadlineLabel({ deadline: at('2026-10-29T09:00:00') }, now), '明天 09:00 到期')
  assert.equal(deadlineLabel({ deadline: at('2026-10-28T18:00:00') }, now), '今天 18:00 到期')
  assert.equal(deadlineState({ deadline: at('2026-11-05T18:00:00') }, now), '')
  assert.equal(deadlineLabel({ deadline: at('2026-10-30T10:00:00') }, now), '10/30（五）10:00')
  assert.equal(deadlineState({ deadline: at('2026-10-27T18:00:00'), done: true }, now), '')
  assert.equal(deadlineState({}, now), '')
})

test('sort: nearest deadline first, none last', () => {
  const list = sortTasks([
    { id: 'none', created_at: '1' },
    { id: 'late', deadline: at('2026-11-05T18:00:00') },
    { id: 'early', deadline: at('2026-10-29T18:00:00') }
  ])
  assert.deepEqual(list.map(t => t.id), ['early', 'late', 'none'])
})

test('subtask progress', () => {
  assert.deepEqual(subtaskProgress({ subtasks: [{ done: true }, { done: false }] }), { done: 1, total: 2 })
  assert.deepEqual(subtaskProgress({}), { done: 0, total: 0 })
})

test('trash days left', () => {
  assert.equal(trashDaysLeft(at('2026-10-27T09:00:00'), now), 29)
  assert.equal(trashDaysLeft(at('2026-10-02T11:05:00'), now), 5)
  assert.equal(trashDaysLeft(at('2026-09-01T00:00:00'), now), 0)
})

test('local time round trip', () => {
  assert.equal(isoToLocal(localToIso('2026-10-29 18:00')), '2026-10-29 18:00')
  assert.equal(localToIso('2026-10-29'), null)
})

test('start date', () => {
  const local = s => new Date(s)
  const t = { start_at: local('2026-11-05T09:00:00').toISOString() }
  // 時程表：週結束（下週一 00:00）前已開始才列出；沒填＝已開始
  assert.equal(startedBefore(t, local('2026-11-02T00:00:00')), false)
  assert.equal(startedBefore(t, local('2026-11-09T00:00:00')), true)
  assert.equal(startedBefore({}, local('2026-11-02T00:00:00')), true)
  // 任務卡：還沒開始才顯示
  assert.equal(startLabel(t, local('2026-11-01T00:00:00').getTime()), '11/05（四）09:00 開始')
  assert.equal(startLabel(t, local('2026-11-05T09:00:00').getTime()), '')
  assert.equal(startLabel({}), '')
})
