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
          <router-link v-if="auth.isAdmin" :to="{ name: 'admin' }" class="nav-item" active-class="nav-active">
            <q-icon name="admin_panel_settings" size="22px" />管理
            <span v-if="auth.pendingCount" class="badge" :aria-label="`${auth.pendingCount} 個待核准`">{{ auth.pendingCount }}</span>
          </router-link>
        </nav>

        <q-space />

        <div v-if="auth.user" class="me row items-center no-wrap">
          <q-avatar size="36px" class="avatar">
            <img v-if="auth.user.picture" :src="auth.user.picture" alt="" referrerpolicy="no-referrer">
            <span v-else>{{ (auth.user.name || auth.user.email || '?').slice(0, 1) }}</span>
          </q-avatar>
          <div class="column col" style="min-width: 0">
            <span class="text-weight-bold ellipsis" style="font-size: 14px">{{ auth.user.name }}</span>
            <span class="text-sub ellipsis" style="font-size: 12px">{{ auth.user.email }}</span>
          </div>
          <q-btn flat round icon="logout" aria-label="登出" @click="auth.logout()">
            <q-tooltip>登出</q-tooltip>
          </q-btn>
        </div>

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
import { onMounted, ref } from 'vue'
import { setDark } from '../utils/theme'
import { useAuthStore } from '../stores/auth'

const drawer = ref(false)
const auth = useAuthStore()

onMounted(() => auth.loadPending())

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
.badge {
  margin-left: auto; min-width: 22px; height: 22px; padding: 0 6px; box-sizing: border-box;
  border-radius: 999px; border: 2px solid var(--line); background: var(--danger); color: var(--on-color);
  font-size: 12px; font-weight: 700; display: flex; align-items: center; justify-content: center;
}
.me { gap: 10px; padding: 10px 8px; margin-bottom: 10px; border-top: 2px dashed var(--grid); }
.avatar { border: 2px solid var(--line); background: var(--soft); font-weight: 700; flex-shrink: 0; }
.nav-active, .nav-active:hover {
  background: var(--accent); color: var(--on-color);
  border-color: var(--line); box-shadow: 3px 3px 0 var(--edge); font-weight: 700;
}
</style>
