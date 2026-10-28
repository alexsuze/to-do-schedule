import { createRouter, createWebHashHistory } from 'vue-router'

// 使用 hash 模式，避免 GitHub Pages 子路徑重新整理 404
const routes = [
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'tasks', component: () => import('../pages/TasksPage.vue') },
      { path: 'timeline', name: 'timeline', component: () => import('../pages/TimelinePage.vue') },
      { path: 'stats', name: 'stats', component: () => import('../pages/ComingSoonPage.vue'), meta: { title: '統計', stage: 3 } },
      { path: 'trash', name: 'trash', component: () => import('../pages/TrashPage.vue') },
      { path: 'settings', name: 'settings', component: () => import('../pages/SettingsPage.vue') }
    ]
  }
]

export default createRouter({
  history: createWebHashHistory(),
  routes
})
