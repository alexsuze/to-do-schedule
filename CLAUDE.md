# CLAUDE.md — to-do-schedule

多人（各用各的）工作事項管理系統：Google 登入＋管理員核准；任務卡 + 週時程表工時紀錄 + 統計 + Email 每日摘要。2026-10-28 打掉重練（舊版最後一版為 git commit a586657）。
使用者以**繁體中文**溝通；對技術術語不熟，說明時用白話並提供 A/B 選項。

## 必讀文件
- `Doc/工作事項管理系統-需求書.md`：新版需求（v1.0 草稿，任務 + 工時紀錄）。任何功能實作以此為準。
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
| 登入 | Google 登入（前端 Google Identity Services 取得 ID token → 後端 `/api/auth/google` 驗證並發通行證），`AuthFilter` 檢查 `Authorization: Bearer`；新帳號需管理員核准 | 環境變數 `GOOGLE_CLIENT_ID`、`ADMIN_EMAIL`、`JWT_SECRET` |

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
cd frontend && npm test                         # utils 純函式測試（Node 內建）
# 後端（本機無 mvn，用 wrapper；本機 Java 26 可執行，目標版本 21）
cd backend && ./mvnw spring-boot:run            # Windows: mvnw.cmd
cd backend && ./mvnw -q -DskipTests package     # 驗證可打包
```
本機沒有 MongoDB，後端 `/actuator/health` 會 DOWN 屬正常；`/actuator/health/liveness` 與 `/api/ping` 可用。

## 安全（GitHub 倉庫為公開，2026-10-28 起）
- **任何提交都會被公開看到**：密碼、API 金鑰、資料庫連線字串、Resend 金鑰、`JWT_SECRET` 一律只放環境變數（Railway Variables、GitHub Secrets），不可寫進程式、文件、測試或範例檔；`.env.example` 只能放假值。
- 本機 `.env`、`.env.local`、`.claude/` 已被 `.gitignore` 排除，不可提交。
- 每次 commit 前用 `git diff --cached --name-only` 確認只包含本次要提交的檔案（曾因先前已 `git rm` 的檔案被一起帶進無關 commit 而需還原）。
- 可公開的資訊：Google 用戶端 ID、Railway 伺服器網址、GitHub Pages 網址（本來就在前端程式中）；`alex801221@gmail.com` 公開於隱私權頁與 commit 作者資訊（使用者已知悉）。
- 安全靠後端把關：Google 登入＋管理員核准＋資料依 `user_id` 隔離；前端程式可被任何人看到，不可在前端放任何機密或只靠前端擋權限。
- 部署 workflow 只在 push `main` 或手動觸發時執行，fork 的 PR 拿不到 Secrets；新增 workflow 時不要用 `pull_request_target` 搭配 Secrets。
- 機密掃描指令與上次結果見 `Doc/開發進度.md`「機密掃描與公開倉庫」。

## 慣例
- 所有後端 API 路徑以 `/api/` 開頭，回傳 JSON（欄位 snake_case）；前端一律透過 `frontend/src/services/api.js` 呼叫（已自動帶通行證，401 自動回登入頁）。
- 資料隔離：所有使用者資料都有 `user_id`，controller 以 `@RequestAttribute(AuthFilter.USER) User` 取得目前使用者並傳給 service；別人的資料一律回「找不到」。
- 後端業務錯誤丟 `ResponseStatusException` 並帶中文訊息（Problem Details 格式，訊息在 `detail`），前端 store 統一 Notify 顯示。
- 資料模型欄位以 snake_case 存於 Mongo。
- 軟刪除：`deleted_at` 非 null 即視為刪除，查詢需排除。
- 時間以 UTC 存，前端依設定時區顯示。
- 文件與 UI 文字使用繁體中文。
- 不主動 commit，由使用者指示。
