# MongoDB 資料結構

資料庫：`todo_schedule`（MongoDB Atlas）。欄位一律 snake_case，時間以 UTC 存。
API JSON 欄位與 Mongo 欄位相同（snake_case）。不建索引（資料量小，且 `auto-index-creation` 會讓本機沒 Mongo 時無法啟動）。

## tasks（階段 1）
任務卡。

| 欄位 | 型別 | 說明 |
|------|------|------|
| _id | ObjectId | |
| title | string | 1～100 字 |
| description | string | 可空字串 |
| category_id | string / null | categories 的 _id（hex 字串）；null = 未分類 |
| deadline | Date / null | 死線 |
| subtasks | array | `{ id, name, done }`；id 由前端產生 UUID，陣列順序即顯示順序；空白名稱的項目儲存時略過 |
| done | boolean | |
| completed_at | Date / null | 改為完成時寫入、改回未完成時清除；統計「期間內完成數」用 |
| deleted_at | Date / null | 非 null 即在回收桶；滿 30 天由排程（每天 03:00 台北時間）永久刪除 |
| created_at / updated_at | Date | Spring Data Auditing 自動寫入 |

## categories（階段 1）
任務分類。

| 欄位 | 型別 | 說明 |
|------|------|------|
| _id | ObjectId | |
| name | string | 1～30 字 |
| color | string | `#RRGGBB`（存大寫） |
| order | int | 顯示順序，越小越前面 |

刪除分類為直接刪除，該分類的任務（含回收桶中的）`category_id` 改為 null。

## 尚未建立
- `time_blocks`（階段 2）、`settings`（階段 4）：欄位見需求書第 6 節，實作時補上。
