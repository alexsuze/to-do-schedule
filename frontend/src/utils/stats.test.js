import { test } from 'node:test'
import assert from 'node:assert/strict'
import { periodRange, buckets, byCategory, donutGradient, taskCounts, inRange, totalMinutes, workedDays } from './stats.js'

const local = s => new Date(s)
const block = (task, s, e) => ({ task_id: task, start_at: local(s).toISOString(), end_at: local(e).toISOString() })
const now = local('2026-10-28T10:00:00')

test('period ranges', () => {
  const w = periodRange('week', now)
  assert.equal(w.from.getTime(), local('2026-10-26T00:00:00').getTime())
  assert.equal(w.to.getTime(), local('2026-11-02T00:00:00').getTime())
  const m = periodRange('month', now)
  assert.equal(m.from.getTime(), local('2026-10-01T00:00:00').getTime())
  assert.equal(m.to.getTime(), local('2026-11-01T00:00:00').getTime())
  const c = periodRange('custom', now, { from: '2026-10-01', to: '2026-10-03' })
  assert.equal(c.from.getTime(), local('2026-10-01T00:00:00').getTime())
  assert.equal(c.to.getTime(), local('2026-10-04T00:00:00').getTime())
})

const blocks = [
  block('a', '2026-10-26T09:00:00', '2026-10-26T10:30:00'),
  block('b', '2026-10-28T13:00:00', '2026-10-28T14:00:00'),
  block('c', '2026-10-28T15:00:00', '2026-10-28T15:30:00'),
  block('a', '2026-11-03T09:00:00', '2026-11-03T10:00:00') // 下週，不算
]

test('totals and daily buckets', () => {
  const { from, to } = periodRange('week', now)
  const week = inRange(blocks, from, to)
  assert.equal(totalMinutes(week), 180)
  assert.equal(workedDays(week), 2) // 週一、週三有排；週三兩段只算一天
  assert.equal(workedDays([]), 0)
  const b = buckets(week, from, to)
  assert.equal(b.perWeek, false)
  assert.deepEqual(b.list.map(x => x.minutes), [90, 0, 90, 0, 0, 0, 0])
  assert.deepEqual(b.list.map(x => x.label), ['一', '二', '三', '四', '五', '六', '日'])
})

test('long ranges group by week', () => {
  const { from, to } = periodRange('custom', now, { from: '2026-10-01', to: '2026-11-30' })
  const b = buckets(inRange(blocks, from, to), from, to)
  assert.equal(b.perWeek, true)
  assert.equal(b.list[0].from.getTime(), from.getTime()) // 第一週從期間起點算
  assert.equal(b.list.reduce((n, x) => n + x.minutes, 0), 240)
})

test('hours by category, uncategorized included', () => {
  const tasks = [{ id: 'a', category_id: 'c1' }, { id: 'b', category_id: null }, { id: 'c', category_id: 'gone' }]
  const rows = byCategory(blocks.slice(0, 3), tasks, [{ id: 'c1', name: '專案A', color: '#8FD3FF' }])
  // 已刪除的分類（gone）算進未分類；同時數時分類在前（穩定排序）
  assert.deepEqual(rows.map(r => [r.name, r.minutes]), [['專案A', 90], ['未分類', 90]])
  assert.equal(rows.reduce((n, r) => n + r.minutes, 0), 180)
  assert.equal(donutGradient([], '#fff'), 'none')
  assert.match(donutGradient(rows, '#fff'), /^conic-gradient\(/)
})

test('task counts', () => {
  const { from, to } = periodRange('week', now)
  const c = taskCounts([
    { done: false, deadline: local('2026-10-27T18:00:00').toISOString() },
    { done: false },
    { done: true, completed_at: local('2026-10-27T12:00:00').toISOString() },
    { done: true, completed_at: local('2026-10-01T12:00:00').toISOString() }
  ], from, to, now)
  assert.deepEqual(c, { open: 2, done: 2, overdue: 1, doneInPeriod: 1 })
})
