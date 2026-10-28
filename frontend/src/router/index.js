import { createRouter, createWebHashHistory } from 'vue-router'
import { authToken, loadSession } from '../services/session'

// 使用 hash 模式，避免 GitHub Pages 子路徑重新整理 404
const routes = [
  { path: '/login', name: 'login', component: () => import('../pages/LoginPage.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'tasks', component: () => import('../pages/TasksPage.vue') },
      { path: 'timeline', name: 'timeline', component: () => import('../pages/TimelinePage.vue') },
      { path: 'stats', name: 'stats', component: () => import('../pages/StatsPage.vue') },
      { path: 'trash', name: 'trash', component: () => import('../pages/TrashPage.vue') },
      { path: 'settings', name: 'settings', component: () => import('../pages/SettingsPage.vue') },
      { path: 'admin', name: 'admin', component: () => import('../pages/AdminPage.vue'), meta: { admin: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 沒登入只能看登入頁；已登入就不用再看登入頁；管理頁只給管理員（後端也會擋）
router.beforeEach(to => {
  const loggedIn = !!authToken()
  if (!to.meta.public && !loggedIn) return { name: 'login' }
  if (to.name === 'login' && loggedIn) return { name: 'tasks' }
  if (to.meta.admin && !loadSession().user?.admin) return { name: 'tasks' }
})

export default router
