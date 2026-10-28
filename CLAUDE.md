# CLAUDE.md — to-do-schedule

單一使用者的工作事項管理系統。2026-10-28 打掉重練，功能重新設計中（舊版最後一版為 git commit a586657）。
使用者以**繁體中文**溝通；對技術術語不熟，說明時用白話並提供 A/B 選項。

## 必讀文件
- 需求書：重新設計中，尚未撰寫。確定後放 `Doc/`，任何功能實作以此為準。
- `Doc/開發進度.md`：目前進度、已完成項目、下一步待辦。**每次完成一個階段都要更新此檔。**
- `Doc/MongoDB資料結構.md`：所有 collection 的欄位、型別、用途與索引。**新增或修改欄位時同步更新。**
- `README.md`：本機啟動與部署步驟。

## 技術架構（已定案，勿更動）
| 層 | 技術 | 部署 |
|----|------|------|
| 前端 | Vite + Vue 3 + Pinia + Quasar（非 Quasar CLI），hash 路由 | GitHub Pages，`.github/workflows/deploy-frontend.yml` |
| 後端 | Java 21 + Spring Boot 4.1.1（Maven Wrapper） | Railway，Root Directory = `backend`，設定於 `backend/railway.toml` |
| 資料庫 | MongoDB Atlas 免費層 | 環境變數 `MONGODB_URI` |
| Email | Resend，寄件者 `onboarding@resend.dev`（無自有網域，只能寄到 Resend 帳號 Email） | 環境變數 `RESEND_API_KEY` |
| 存取保護 | 簡易 API Token，Header `X-API-Token`，後端 `ApiTokenFilter` 比對 `API_TOKEN` | — |

## Repo 結構（monorepo）
```
Doc/        需求書、開發進度
frontend/   src/{router,layouts,pages,components,stores,services,utils,css}
backend/    src/main/java/com/todoschedule/{config,controller,model,repository,service}
.github/workflows/
```

## 開發指令
```bash
# 前端
cd frontend && npm install && npm run dev      # http://localhost:5173
cd frontend && npm run build                    # 驗證可建置
# 後端（本機無 mvn，用 wrapper；本機 Java 26 可執行，目標版本 21）
cd backend && ./mvnw spring-boot:run            # Windows: mvnw.cmd
cd backend && ./mvnw -q -DskipTests package     # 驗證可打包
```
本機沒有 MongoDB，後端 `/actuator/health` 會 DOWN 屬正常；`/actuator/health/liveness` 與 `/api/ping` 可用。

## 慣例
- 所有後端 API 路徑以 `/api/` 開頭，回傳 JSON（欄位 snake_case）；前端一律透過 `frontend/src/services/api.js` 呼叫（已自動帶 Token）。
- 後端業務錯誤丟 `ResponseStatusException` 並帶中文訊息（Problem Details 格式，訊息在 `detail`），前端 store 統一 Notify 顯示。
- 資料模型欄位以 snake_case 存於 Mongo。
- 軟刪除：`deleted_at` 非 null 即視為刪除，查詢需排除。
- 時間以 UTC 存，前端依設定時區顯示。
- 文件與 UI 文字使用繁體中文。
- 不主動 commit，由使用者指示。
