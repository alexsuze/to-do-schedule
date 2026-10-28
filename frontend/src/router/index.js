import { createRouter, createWebHashHistory } from 'vue-router'

// 使用 hash 模式，避免 GitHub Pages 子路徑重新整理 404
const routes = [
  { path: '/', name: 'home', component: () => import('../pages/HomePage.vue') }
]

export default createRouter({
  history: createWebHashHistory(),
  routes
})
