# MongoDB 資料結構

| 項目 | 內容 |
|------|------|
| 資料庫名稱 | `todo_schedule`（Atlas cluster `todolist`） |
| 最後更新 | 2026-09-09 |
| 依據 | `工作事項管理系統-需求書.md` 第 6 節 |

本文件整理所有 collection 的欄位、型別、用途與建立階段，供後續開發對照。欄位名稱一律 **snake_case**；時間一律以 **UTC** 儲存，前端依設定時區顯示。

## 共通規則

- **軟刪除**：`deleted_at` 為 null 代表正常資料；非 null 代表已刪除、進回收桶。所有一般查詢都必須加上 `deleted_at == null` 條件。回收桶保留 30 天後由後端排程永久刪除。
- **時間戳**：`created_at` 建立時填入；`updated_at` 每次修改自動更新。
- **ID**：`_id` 皆為 MongoDB ObjectId，API 以 24 碼十六進位字串傳遞。
- **排序值 `order`**：從 0 開始的整數，數字小的排前面；只在同一群組內比較（例如同一看板的欄位、同一欄位內的事項）。

## 各 collection 總覽

| collection | 用途 | 建立階段 |
|-----------|------|---------|
| `boards` | 看板與其內嵌的欄位 | 階段 1 |
| `tasks` | 工作事項 | 階段 2 |
| `settings` | 全域設定（只有一筆） | 階段 5 |
| `notifications` | Email 通知排程與寄送紀錄 | 階段 5 |

---

## boards（看板）

一個看板等於一個專案或主題，內含若干欄位（狀態）。欄位數量少，直接內嵌在看板文件中，不另開 collection。

| 欄位 | 型別 | 必填 | 用途 |
|------|------|------|------|
| `_id` | ObjectId | 系統 | 看板識別碼 |
| `name` | String | 是 | 看板名稱，1～50 字 |
| `color` | String | 否 | 側邊欄辨識用色碼，如 `#1976d2`；null 時前端用預設色 |
| `order` | Int | 是 | 側邊欄顯示順序，新看板為目前最大值 +1 |
| `last_view_mode` | String | 是 | 該看板最後使用的檢視模式：`kanban` / `timeline` / `list`，預設 `kanban` |
| `columns` | Array\<Column\> | 是 | 內嵌欄位清單，至少一個，結構見下表 |
| `deleted_at` | Date / null | 系統 | 軟刪除時間 |
| `created_at` | Date | 系統 | 建立時間（UTC） |
| `updated_at` | Date | 系統 | 最後修改時間（UTC） |

### Column（內嵌於 boards.columns）

| 欄位 | 型別 | 必填 | 用途 |
|------|------|------|------|
| `_id` | ObjectId | 系統 | 欄位識別碼；`tasks.column_id` 指向此值，欄位改名後事項不受影響 |
| `name` | String | 是 | 欄位名稱，1～30 字 |
| `order` | Int | 是 | 欄位由左到右順序 |
| `is_done_type` | Boolean | 是 | 是否為「完成類型」。事項移入此類欄位會自動填 `tasks.completed_at` 並取消通知；移出則清除並重新排程 |

### 業務規則
- 新建看板自動帶三個欄位：「待辦」(0)、「進行中」(1)、「完成」(2, `is_done_type = true`)。
- 系統首次啟動若無任何未刪除看板，自動建立名為「我的工作」的看板。
- 至少保留一個未刪除看板，不可刪除最後一個。
- 刪除看板時，其下所有事項一併軟刪除（階段 2 實作）。
- 刪除欄位時若仍有事項，需由使用者選擇「一併軟刪除」或「移至其他欄位」（階段 2 實作）。

### 索引
- `{ deleted_at: 1, order: 1 }`：側邊欄列表查詢直接排序。

### 範例
```json
{
  "_id": ObjectId("66e0000000000000000000a0"),
  "name": "我的工作",
  "color": "#1976d2",
  "order": 0,
  "last_view_mode": "kanban",
  "columns": [
    { "_id": ObjectId("66e0000000000000000000a1"), "name": "待辦",   "order": 0, "is_done_type": false },
    { "_id": ObjectId("66e0000000000000000000a2"), "name": "進行中", "order": 1, "is_done_type": false },
    { "_id": ObjectId("66e0000000000000000000a3"), "name": "完成",   "order": 2, "is_done_type": true  }
  ],
  "deleted_at": null,
  "created_at": ISODate("2026-09-09T03:30:00Z"),
  "updated_at": ISODate("2026-09-09T03:30:00Z")
}
```

---

## tasks（工作事項）— 階段 2

系統最小管理單位，每筆必屬於一個看板與該看板的一個欄位。

| 欄位 | 型別 | 必填 | 用途 |
|------|------|------|------|
| `_id` | ObjectId | 系統 | 事項識別碼 |
| `board_id` | ObjectId | 是 | 所屬看板（`boards._id`） |
| `column_id` | ObjectId | 是 | 所在欄位（`boards.columns._id`），即事項狀態；新增時預設看板第一個欄位 |
| `title` | String | 是 | 標題，1～100 字 |
| `description` | String / null | 否 | 描述，支援多行 |
| `priority` | String | 是 | 優先度：`high` / `medium` / `low`，預設 `medium` |
| `start_date` | Date / null | 否 | 開始日期（只取日期部分），時程表起點 |
| `due_at` | Date / null | 否 | 到期日期時間（UTC），時程表終點，亦為通知依據；不可早於 `start_date` |
| `order` | Int | 是 | 同一欄位內的排序 |
| `tags` | Array\<String\> | 是 | 標籤，可為空陣列 |
| `notification_override` | Object / null | 否 | 事項專屬通知設定；null 表示沿用全域設定，結構見下表 |
| `completed_at` | Date / null | 系統 | 移入完成類型欄位時自動填入，移出時清除 |
| `deleted_at` | Date / null | 系統 | 軟刪除時間 |
| `created_at` | Date | 系統 | 建立時間 |
| `updated_at` | Date | 系統 | 最後修改時間 |

### notification_override（內嵌於 tasks）

| 欄位 | 型別 | 用途 |
|------|------|------|
| `enabled` | Boolean | false 表示此事項關閉通知 |
| `lead_times` | Array\<String\> | 專屬提前時間，值同 `settings.default_lead_times` |

### 業務規則
- 復原時若原欄位已不存在，回到看板第一個欄位；若看板已刪除，需先復原看板。
- 復原後若仍有未來的 `due_at` 且未完成，重新建立通知排程。
- 只對「有 `due_at`」且「不在完成類型欄位」且「未刪除」的事項排程通知。

### 索引
- `{ board_id: 1, deleted_at: 1 }`：看板內事項列表。
- `{ due_at: 1 }`：時程表與通知掃描。
- `{ deleted_at: 1 }`：回收桶列表與 30 天清理。

---

## settings（全域設定）— 階段 5

單一使用者系統，此 collection **永遠只有一筆文件**。後端以固定 `_id`（例如 `"global"`）讀寫，不存在時以預設值建立。

| 欄位 | 型別 | 預設值 | 用途 |
|------|------|--------|------|
| `_id` | String | `"global"` | 固定識別碼 |
| `notify_email` | String / null | null | 通知收件地址；未驗證網域前只能填註冊 Resend 的 Email |
| `notifications_enabled` | Boolean | true | Email 通知總開關 |
| `default_lead_times` | Array\<String\> | `["1d"]` | 預設提前時間，可複選；可用值 `0m` `15m` `1h` `1d` `3d` `1w` |
| `daily_digest_enabled` | Boolean | false | 是否寄每日摘要 |
| `daily_digest_time` | String | `"09:00"` | 每日摘要寄送時間（HH:mm，依 `timezone`） |
| `timezone` | String | `"Asia/Taipei"` | 到期時間與通知時間計算用時區 |
| `last_board_id` | ObjectId / null | null | 最後開啟的看板，進入系統時預設顯示（階段 1～4 暫存於瀏覽器 localStorage） |
| `updated_at` | Date | — | 最後修改時間 |

---

## notifications（通知排程與紀錄）— 階段 5

每一筆代表「某事項在某時間要寄一封信」。排程器每分鐘掃描 `status = pending` 且 `scheduled_at <= now` 的資料寄送。

| 欄位 | 型別 | 必填 | 用途 |
|------|------|------|------|
| `_id` | ObjectId | 系統 | 識別碼 |
| `task_id` | ObjectId / null | 否 | 對象事項；每日摘要為 null |
| `type` | String | 是 | `reminder`（到期提醒）/ `daily_digest`（每日摘要） |
| `lead_time` | String / null | 否 | 提前時間，如 `"1d"`；每日摘要為 null |
| `scheduled_at` | Date | 是 | 預計寄送時間（UTC） |
| `sent_at` | Date / null | 系統 | 實際寄出時間 |
| `status` | String | 是 | `pending` / `sent` / `failed` / `cancelled` |
| `retry_count` | Int | 系統 | 已重試次數，最多 3 次，間隔 5 分鐘 |
| `resend_message_id` | String / null | 系統 | Resend 回傳的訊息 ID |
| `error_message` | String / null | 系統 | 最後一次失敗原因 |
| `created_at` | Date | 系統 | 建立時間 |

### 業務規則
- 同一事項同一 `lead_time` 只能有一筆 `pending`，避免重複寄送。
- 事項 `due_at` 變更時，重算所有 `pending` 通知的 `scheduled_at`。
- 事項完成、刪除或所屬看板刪除時，將 `pending` 改為 `cancelled`。
- 後端停機恢復後，`scheduled_at` 已過但事項仍未到期者立即補寄；已到期者併入每日摘要。
- 設定頁顯示最近 50 筆紀錄。

### 索引
- `{ status: 1, scheduled_at: 1 }`：排程器掃描。
- `{ task_id: 1 }`：事項變更時找出其通知。

---

## 關聯圖

```
boards ──< tasks ──< notifications
  │            │
  └ columns    └ notification_override（內嵌）
    （內嵌）
settings（獨立，單一文件）
```

## 變更紀錄

| 日期 | 內容 |
|------|------|
| 2026-09-09 | 初版，依需求書 v0.5 整理；補充 Column `_id`、settings 固定 `_id`、各索引用途與階段標示 |
