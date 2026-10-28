<template>
  <q-layout view="lHh LpR lFf">
    <!-- 窄螢幕才顯示上方列，用來開側邊欄 -->
    <q-header v-if="$q.screen.lt.md" class="bg-surface" style="border-bottom: 2px solid var(--line)">
      <q-toolbar>
        <q-btn flat round icon="menu" aria-label="開啟選單" @click="drawer = !drawer" />
        <q-toolbar-title class="text-weight-bold">工作事項管理</q-toolbar-title>
      </q-toolbar>
    </q-header>

    <q-drawer v-model="drawer" show-if-above :width="240" class="side">
      <div class="column no-wrap full-height q-pa-md">
        <div class="row items-center no-wrap q-pb-lg q-pt-xs" style="gap: 10px">
          <div class="logo"><q-icon name="check" size="26px" /></div>
          <div class="column">
            <span class="text-weight-bold" style="font-size: 18px">工作事項管理</span>
            <span class="text-sub" style="font-size: 13px">今天也慢慢來</span>
          </div>
        </div>

        <nav aria-label="主選單" class="column q-gutter-y-sm">
          <router-link v-for="n in NAV" :key="n.name" :to="{ name: n.name }" class="nav-item" active-class="nav-active" exact-active-class="nav-active">
            <q-icon :name="n.icon" size="22px" />{{ n.label }}
          </router-link>
        </nav>

        <q-space />

        <q-btn no-caps unelevated class="fun-btn bg-surface full-width" :aria-label="'切換深色或淺色模式'" @click="setDark(!$q.dark.isActive)">
          <q-icon :name="$q.dark.isActive ? 'dark_mode' : 'light_mode'" size="20px" class="q-mr-sm" />
          {{ $q.dark.isActive ? '深色模式' : '淺色模式' }}
          <q-space />
          <span class="text-sub text-weight-regular" style="font-size: 12px">點我切換</span>
        </q-btn>
      </div>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { ref } from 'vue'
import { setDark } from '../utils/theme'

const drawer = ref(false)

const NAV = [
  { name: 'tasks', label: '任務', icon: 'task_alt' },
  { name: 'timeline', label: '時程表', icon: 'calendar_month' },
  { name: 'stats', label: '統計', icon: 'bar_chart' },
  { name: 'trash', label: '回收桶', icon: 'delete_outline' },
  { name: 'settings', label: '設定', icon: 'tune' }
]
</script>

<style scoped>
.side { background: var(--side); border-right: 2px solid var(--line); }
.logo {
  width: 44px; height: 44px; flex-shrink: 0;
  border-radius: 14px; border: 2px solid var(--line); box-shadow: 3px 3px 0 var(--edge);
  background: var(--accent); color: var(--on-color);
  display: flex; align-items: center; justify-content: center;
  transform: rotate(-6deg);
}
.nav-item {
  display: flex; align-items: center; gap: 12px;
  min-height: 48px; padding: 0 14px;
  border-radius: 14px; border: 2px solid transparent;
  color: var(--ink); text-decoration: none; font-size: 16px;
}
.nav-item:hover { background: var(--soft); }
.nav-active, .nav-active:hover {
  background: var(--accent); color: var(--on-color);
  border-color: var(--line); box-shadow: 3px 3px 0 var(--edge); font-weight: 700;
}
</style>
