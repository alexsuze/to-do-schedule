import { test } from 'node:test'
import assert from 'node:assert/strict'
import { toSpecialDays, dateKey } from './holidays.js'

test('keeps only special days', () => {
  const days = toSpecialDays([
    { date: '20261009', week: '五', isHoliday: true, description: '補假' },
    { date: '20261010', week: '六', isHoliday: true, description: '國慶日' }, // 週末遇國定假日也標
    { date: '20261011', week: '日', isHoliday: true, description: '' }, // 一般週末不標
    { date: '20261012', week: '一', isHoliday: false, description: '' }, // 一般上班日不標
    { date: '20260214', week: '六', isHoliday: false, description: '補行上班' }, // 補班
    { date: '20260101', week: '四', isHoliday: true, description: '' } // 平日放假但沒名稱
  ])
  assert.deepEqual(days, {
    20261009: { off: true, name: '補假' },
    20261010: { off: true, name: '國慶日' },
    20260214: { off: false, name: '補班' },
    20260101: { off: true, name: '放假' }
  })
})

test('date key', () => {
  assert.equal(dateKey(new Date(2026, 9, 9)), '20261009')
})
