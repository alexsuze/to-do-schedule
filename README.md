# 工作事項管理系統（to-do-schedule）

單一使用者的工作事項管理系統，提供看板 / 時程表 / 列表三種檢視模式與 Email 到期提醒。
需求書見 [Doc/工作事項管理系統-需求書.md](Doc/工作事項管理系統-需求書.md)。

## Repo 結構

```
to-do-schedule/
├── Doc/                 # 需求書與設計文件
├── frontend/            # Vue 3 + Pinia + Quasar（Vite）
├── backend/             # Java 21 + Spring Boot 4（Maven）
└── .github/workflows/   # 前端 GitHub Pages 自動部署
```

## 本機開發

### 需要安裝
- Node.js 20+
- Java 21+（backend 內含 Maven Wrapper，不需另裝 Maven）
- MongoDB 連線字串（MongoDB Atlas 免費層，或本機 MongoDB）

### 後端
```bash
cd backend
# 設定環境變數（見 backend/.env.example），至少需要 MONGODB_URI
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```
啟動後：
- `GET http://localhost:8080/actuator/health/liveness` 應回 `{"status":"UP"}`
- `GET http://localhost:8080/api/ping` 需帶 Header `X-API-Token: <API_TOKEN>`（預設 `dev-token`）

### 前端
```bash
cd frontend
cp .env.example .env.local      # 依需要修改 API 網址與 Token
npm install
npm run dev                     # http://localhost:5173
```
首頁的「測試後端連線」按鈕可驗證前後端與 Token 是否正確。

## 部署

### 前端 → GitHub Pages
1. GitHub repo → Settings → Pages → Source 選 **GitHub Actions**。
2. Settings → Secrets and variables → Actions 新增：
   - `VITE_API_BASE_URL`：Railway 後端網址（例如 `https://xxx.up.railway.app`）
   - `VITE_API_TOKEN`：與後端 `API_TOKEN` 相同的字串
3. push 到 `main` 且 `frontend/` 有變動時，`.github/workflows/deploy-frontend.yml` 會自動建置並部署。
4. 網址為 `https://<帳號>.github.io/to-do-schedule/`。

### 後端 → Railway
1. Railway 新建專案 → Deploy from GitHub repo → 選此 repo。
2. 服務 Settings → **Root Directory** 設為 `backend`（monorepo 必要）。
3. Variables 新增：

   | 變數 | 說明 |
   |------|------|
   | `MONGODB_URI` | Atlas 連線字串 |
   | `API_TOKEN` | 自訂一組隨機字串 |
   | `CORS_ALLOWED_ORIGIN` | `https://<帳號>.github.io` |
   | `RESEND_API_KEY` | Resend API Key |
   | `RESEND_FROM_EMAIL` | `onboarding@resend.dev` |
   | `FRONTEND_URL` | `https://<帳號>.github.io/to-do-schedule/` |

4. push 到 `main` 即自動部署，建置與啟動指令定義於 `backend/railway.toml`。

### MongoDB Atlas
1. 建立免費 M0 cluster，新增資料庫使用者。
2. Network Access 允許 `0.0.0.0/0`（Railway 出口 IP 不固定）。
3. 取得連線字串填入 `MONGODB_URI`，資料庫名稱建議 `todo_schedule`。

### Resend
1. 註冊 Resend 並建立 API Key。
2. 未驗證自有網域時，只能寄送到註冊 Resend 的 Email，系統「收件 Email」設定需填該地址。
