import { test } from 'node:test'
import assert from 'node:assert/strict'
import { weekStart, addDays, slotTime, slotOf, blockSlots, formatHours, formatSlot, minutesByTask, overlaps, visibleRange } from './timeline.js'

const local = s => new Date(s) // 無時區字串 = 本地時間
const block = (id, task, s, e) => ({ id, task_id: task, start_at: local(s).toISOString(), end_at: local(e).toISOString() })

test('week starts on Monday', () => {
  assert.equal(weekStart(local('2026-10-28T15:00:00')).getTime(), local('2026-10-26T00:00:00').getTime())
  assert.equal(weekStart(local('2026-11-01T23:00:00')).getTime(), local('2026-10-26T00:00:00').getTime()) // 週日
  assert.equal(addDays(local('2026-10-26T00:00:00'), 7).getTime(), local('2026-11-02T00:00:00').getTime())
})

test('slots', () => {
  const day = local('2026-10-28T00:00:00')
  assert.equal(slotTime(day, 19).getTime(), local('2026-10-28T09:30:00').getTime())
  assert.equal(slotTime(day, 48).getTime(), local('2026-10-29T00:00:00').getTime())
  assert.equal(slotOf(local('2026-10-28T09:30:00')), 19)
  assert.deepEqual(blockSlots(block('a', 't', '2026-10-28T23:00:00', '2026-10-29T00:00:00')), { start: 46, len: 2 })
  assert.equal(formatSlot(19), '09:30')
})

test('hours', () => {
  assert.equal(formatHours(90), '1.5h')
  assert.equal(formatHours(120), '2h')
  assert.equal(formatHours(0), '0h')
  const m = minutesByTask([block('a', 't1', '2026-10-28T09:00:00', '2026-10-28T10:30:00'), block('b', 't1', '2026-10-29T09:00:00', '2026-10-29T09:30:00')])
  assert.equal(m.t1, 120)
})

test('overlap', () => {
  const list = [block('a', 't', '2026-10-28T09:00:00', '2026-10-28T10:00:00')]
  const at = s => local(`2026-10-28T${s}:00`)
  assert.equal(overlaps(list, at('09:30'), at('10:30')), true)
  assert.equal(overlaps(list, at('10:00'), at('10:30')), false) // 緊接著
  assert.equal(overlaps(list, at('08:30'), at('09:00')), false)
  assert.equal(overlaps(list, at('09:00'), at('11:00'), 'a'), false) // 自己延展
})

test('visible range grows to fit blocks', () => {
  const r = { start: 9, end: 18 }
  assert.deepEqual(visibleRange(r, []), r)
  assert.deepEqual(visibleRange(r, [block('a', 't', '2026-10-28T07:30:00', '2026-10-28T08:00:00'), block('b', 't', '2026-10-28T19:00:00', '2026-10-28T20:30:00')]), { start: 7, end: 21 })
})
