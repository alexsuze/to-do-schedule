import { test } from 'node:test'
import assert from 'node:assert/strict'
import { ALL_COMPANIES, resolveCompany, inCompany, splitBlocks, overlapsAny, initialOf } from './company.js'

const list = [
  { id: 'a', archived: true },
  { id: 'b', archived: false },
  { id: 'c', archived: false }
]

test('resolveCompany：記住的可用就用，否則第一家未封存', () => {
  assert.equal(resolveCompany(list, 'c'), 'c')
  assert.equal(resolveCompany(list, 'a'), 'b', '已封存')
  assert.equal(resolveCompany(list, 'zzz'), 'b', '不存在')
  assert.equal(resolveCompany(list, null), 'b')
  assert.equal(resolveCompany([], 'b'), null)
})

test('inCompany 與 splitBlocks、overlapsAny', () => {
  assert.equal(inCompany({ company_id: 'b' }, 'b'), true)
  assert.equal(inCompany({ company_id: 'c' }, 'b'), false)
  assert.equal(inCompany({ company_id: 'c' }, ALL_COMPANIES), true)

  const blocks = [
    { id: 1, task_id: 't1', start_at: '2026-11-09T01:00:00Z', end_at: '2026-11-09T02:00:00Z' },
    { id: 2, task_id: 't2', start_at: '2026-11-09T01:30:00Z', end_at: '2026-11-09T03:00:00Z' },
    { id: 3, task_id: 't2', start_at: '2026-11-09T05:00:00Z', end_at: '2026-11-09T06:00:00Z' }
  ]
  const { mine, others } = splitBlocks(blocks, { t1: 'b', t2: 'c' }, 'b')
  assert.deepEqual(mine.map(b => b.id), [1])
  assert.deepEqual(others.map(b => b.id), [2, 3])
  assert.equal(overlapsAny(mine[0], others), true)
  assert.equal(overlapsAny(others[1], mine), false)
  assert.equal(overlapsAny({ start_at: '2026-11-09T02:00:00Z', end_at: '2026-11-09T02:30:00Z' }, mine), false, '緊接著不算')
})

test('initialOf', () => {
  assert.equal(initialOf('樂天'), '樂')
  assert.equal(initialOf(' abc'), 'A')
  assert.equal(initialOf(''), '?')
})
