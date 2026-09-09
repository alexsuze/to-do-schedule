import { createRouter, createWebHashHistory } from 'vue-router'

// 使用 hash 模式，避免 GitHub Pages 子路徑重新整理 404
const routes = [
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      // 無 id 時由 BoardPage 導向最後開啟的看板
      { path: '', name: 'home', component: () => import('../pages/BoardPage.vue') },
      { path: 'boards/:id', name: 'board', component: () => import('../pages/BoardPage.vue') },
      { path: 'trash', name: 'trash', component: () => import('../pages/TrashPage.vue') },
      { path: 'settings', name: 'settings', component: () => import('../pages/SettingsPage.vue') }
    ]
  }
]

export default createRouter({
  history: createWebHashHistory(),
  routes
})
