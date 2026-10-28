# 工作事項管理系統（to-do-schedule）

多人（各用各的）工作事項管理系統：Google 登入、管理員核准；任務卡 + 週時程表工時紀錄 + 統計 + Email 每日摘要。需求書見 [Doc/工作事項管理系統-需求書.md](Doc/工作事項管理系統-需求書.md)。

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
# 設定環境變數（見 backend/.env.example），至少需要 MONGODB_URI；要登入還需 GOOGLE_CLIENT_ID、ADMIN_EMAIL
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```
啟動後：
- `GET http://localhost:8080/actuator/health/liveness` 應回 `{"status":"UP"}`
- `GET http://localhost:8080/api/ping` 不需登入；其他 `/api/**` 需 Header `Authorization: Bearer <通行證>`（登入後由 `/api/auth/google` 取得）

### 前端
```bash
cd frontend
cp .env.example .env.local      # 填 API 網址與 Google 用戶端 ID
npm install
npm run dev                     # http://localhost:5173
```
開啟首頁會進登入頁，用 `ADMIN_EMAIL` 的 Google 帳號登入後能新增任務卡，代表前後端與資料庫皆已接通。Google 用戶端 ID 的「已授權的 JavaScript 來源」需包含 `http://localhost:5173`。

## 部署

### 前端 → GitHub Pages
1. GitHub repo → Settings → Pages → Source 選 **GitHub Actions**。
2. Settings → Secrets and variables → Actions 新增：
   - `VITE_API_BASE_URL`：Railway 後端網址（例如 `https://xxx.up.railway.app`）
   - `VITE_GOOGLE_CLIENT_ID`：Google OAuth 用戶端 ID（與後端 `GOOGLE_CLIENT_ID` 相同）
3. push 到 `main` 且 `frontend/` 有變動時，`.github/workflows/deploy-frontend.yml` 會自動建置並部署。
4. 網址為 `https://<帳號>.github.io/to-do-schedule/`。

### 後端 → Railway
1. Railway 新建專案 → Deploy from GitHub repo → 選此 repo。
2. 服務 Settings → **Root Directory** 設為 `backend`（monorepo 必要）。
3. Variables 新增：

   | 變數 | 說明 |
   |------|------|
   | `MONGODB_URI` | Atlas 連線字串 |
   | `GOOGLE_CLIENT_ID` | Google OAuth 用戶端 ID |
   | `ADMIN_EMAIL` | 管理員的 Google Email（第一次登入自動成為管理員，並接收多人化前的舊資料） |
   | `JWT_SECRET` | 通行證簽章密鑰，至少 32 字元；未設定時正式環境拒絕啟動。可在 Railway 填 `${{ secret(64) }}` 自動產生 |
   | `MAIL_ALL_USERS` | 每日摘要是否開放給所有人，預設 `false`（Resend 驗證自有網域後才改 `true`） |
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
2. 未驗證自有網域時，只能寄送到註冊 Resend 的 Email，因此每日摘要只開放給管理員（管理員的 Google Email 需與 Resend 帳號 Email 相同）。

### Google 登入（OAuth 用戶端 ID）
1. Google Cloud Console 建立專案 → 「API 和服務」→「OAuth 同意畫面」：使用者類型「外部」，填應用程式名稱與聯絡 Email，範圍只需預設（email、profile、openid），最後「發布應用程式」為正式版。
2. 「憑證」→「建立憑證」→「OAuth 用戶端 ID」→ 類型「網頁應用程式」，「已授權的 JavaScript 來源」加入 `https://<帳號>.github.io` 與 `http://localhost:5173`（不需重新導向 URI）。
3. 取得的用戶端 ID 填入 GitHub Secret `VITE_GOOGLE_CLIENT_ID` 與 Railway `GOOGLE_CLIENT_ID`。
