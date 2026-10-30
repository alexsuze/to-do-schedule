import { test } from 'node:test'
import assert from 'node:assert/strict'
import { dueBlocks, reminderTitle } from './reminder.js'

const local = s => new Date(s)
const block = (id, task, s, e) => ({ id, task_id: task, start_at: local(s).toISOString(), end_at: local(e).toISOString() })

test('due within lead time, not started yet', () => {
  const list = [
    block('a', 't1', '2026-10-30T09:30:00', '2026-10-30T10:00:00'),
    block('b', 't2', '2026-10-30T09:40:00', '2026-10-30T10:00:00'),
    block('c', 't3', '2026-10-30T09:00:00', '2026-10-30T09:30:00')
  ]
  const ids = now => dueBlocks(list, local(now), 5).map(b => b.id)
  assert.deepEqual(ids('2026-10-30T09:24:59'), [])
  assert.deepEqual(ids('2026-10-30T09:25:00'), ['a'])
  assert.deepEqual(ids('2026-10-30T09:29:30'), ['a'])
  assert.deepEqual(ids('2026-10-30T09:30:00'), [])
})

test('continuation of the same task is not a new start', () => {
  const list = [
    block('a', 't1', '2026-10-30T09:30:00', '2026-10-30T10:00:00'),
    block('b', 't1', '2026-10-30T10:00:00', '2026-10-30T10:30:00'),
    block('c', 't2', '2026-10-30T10:00:00', '2026-10-30T10:30:00')
  ]
  assert.deepEqual(dueBlocks(list, local('2026-10-30T09:56:00'), 5).map(b => b.id), ['c'])
})

test('title', () => {
  assert.equal(reminderTitle('寫報告', '公司'), '【公司】寫報告 即將開始')
  assert.equal(reminderTitle('寫報告', ''), '寫報告 即將開始')
})
