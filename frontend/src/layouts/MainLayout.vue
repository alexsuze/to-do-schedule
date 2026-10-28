<template>
  <q-layout view="lHh LpR lFf">
    <!-- 窄螢幕才顯示上方列，用來開側邊欄 -->
    <q-header v-if="$q.screen.lt.md" class="bg-surface" style="border-bottom: 2px solid var(--line)">
      <q-toolbar>
        <q-btn flat round icon="menu" aria-label="開啟選單" @click="drawer = !drawer" />
        <q-toolbar-title class="text-weight-bold">工作事項管理</q-toolbar-title>
      </q-toolbar>
    </q-header>

    <!-- 寬螢幕可收合成只剩圖示（mini），狀態記在瀏覽器；窄螢幕維持滑出式 -->
    <q-drawer v-model="drawer" show-if-above :width="240" :mini="collapsed" :mini-width="76" class="side">
      <div class="column no-wrap full-height" :class="collapsed ? 'q-py-md q-px-sm' : 'q-pa-md'">
        <div class="row items-center no-wrap q-pb-lg q-pt-xs" :class="{ 'justify-center': collapsed }" style="gap: 10px">
          <div class="logo"><q-icon name="check" size="26px" /></div>
          <div v-if="!collapsed" class="column col" style="min-width: 0">
            <span class="text-weight-bold ellipsis" style="font-size: 18px">工作事項管理</span>
            <span class="text-sub" style="font-size: 13px">今天也慢慢來</span>
          </div>
        </div>

        <nav aria-label="主選單" class="column q-gutter-y-sm">
          <router-link
            v-for="n in navItems" :key="n.name" :to="{ name: n.name }"
            class="nav-item" :class="{ mini: collapsed }" active-class="nav-active" :exact-active-class="n.name === 'tasks' ? 'nav-active' : ''"
            :aria-label="collapsed ? n.label : undefined"
          >
            <span class="nav-icon">
              <q-icon :name="n.icon" size="22px" />
              <span v-if="collapsed && n.badge" class="dot" />
            </span>
            <template v-if="!collapsed">
              {{ n.label }}
              <span v-if="n.badge" class="badge" :aria-label="`${n.badge} 個待核准`">{{ n.badge }}</span>
            </template>
            <q-tooltip v-if="collapsed" anchor="center right" self="center left" :offset="[10, 0]">
              {{ n.label }}{{ n.badge ? `（${n.badge} 個待核准）` : '' }}
            </q-tooltip>
          </router-link>
        </nav>

        <q-space />

        <!-- 收合 / 展開（只在寬螢幕） -->
        <q-btn
          v-if="!$q.screen.lt.md"
          flat no-caps class="collapse-btn" :class="{ 'self-center': collapsed }"
          :icon="collapsed ? 'keyboard_double_arrow_right' : 'keyboard_double_arrow_left'"
          :label="collapsed ? undefined : '收合側邊欄'"
          :aria-label="collapsed ? '展開側邊欄' : '收合側邊欄'"
          @click="toggleMini"
        >
          <q-tooltip v-if="collapsed" anchor="center right" self="center left" :offset="[10, 0]">展開側邊欄</q-tooltip>
        </q-btn>

        <div v-if="auth.user" class="me row items-center no-wrap" :class="{ 'justify-center': collapsed }">
          <q-avatar size="36px" class="avatar" :class="{ 'cursor-pointer': collapsed }">
            <img v-if="auth.user.picture" :src="auth.user.picture" alt="" referrerpolicy="no-referrer">
            <span v-else>{{ (auth.user.name || auth.user.email || '?').slice(0, 1) }}</span>
            <!-- 收合時點大頭貼看帳號與登出 -->
            <q-menu v-if="collapsed" anchor="top right" self="bottom left">
              <div class="q-pa-md column" style="gap: 8px; min-width: 200px">
                <span class="text-weight-bold">{{ auth.user.name }}</span>
                <span class="text-sub" style="font-size: 12px">{{ auth.user.email }}</span>
                <q-btn no-caps unelevated class="fun-btn bg-surface" icon="logout" label="登出" @click="auth.logout()" />
              </div>
            </q-menu>
          </q-avatar>
          <template v-if="!collapsed">
            <div class="column col" style="min-width: 0">
              <span class="text-weight-bold ellipsis" style="font-size: 14px">{{ auth.user.name }}</span>
              <span class="text-sub ellipsis" style="font-size: 12px">{{ auth.user.email }}</span>
            </div>
            <q-btn flat round icon="logout" aria-label="登出" @click="auth.logout()">
              <q-tooltip>登出</q-tooltip>
            </q-btn>
          </template>
        </div>

        <q-btn v-if="collapsed" round unelevated class="fun-btn bg-surface self-center" :icon="$q.dark.isActive ? 'dark_mode' : 'light_mode'" aria-label="切換深色或淺色模式" @click="setDark(!$q.dark.isActive)">
          <q-tooltip anchor="center right" self="center left" :offset="[10, 0]">{{ $q.dark.isActive ? '深色模式' : '淺色模式' }}（點我切換）</q-tooltip>
        </q-btn>
        <q-btn v-else no-caps unelevated class="fun-btn bg-surface full-width" aria-label="切換深色或淺色模式" @click="setDark(!$q.dark.isActive)">
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
import { computed, onMounted, ref } from 'vue'
import { useQuasar } from 'quasar'
import { setDark } from '../utils/theme'
import { useAuthStore } from '../stores/auth'

const MINI_KEY = 'sidebarMini'

const $q = useQuasar()
const drawer = ref(false)
const auth = useAuthStore()

// 收合狀態記在瀏覽器；窄螢幕一律用完整版（滑出式）
function loadMini () {
  try { return localStorage.getItem(MINI_KEY) === '1' } catch { return false }
}
const mini = ref(loadMini())
const collapsed = computed(() => mini.value && !$q.screen.lt.md)
function toggleMini () {
  mini.value = !mini.value
  try { localStorage.setItem(MINI_KEY, mini.value ? '1' : '0') } catch { /* 不記也能用 */ }
}

onMounted(() => auth.loadPending())

const NAV = [
  { name: 'tasks', label: '任務', icon: 'task_alt' },
  { name: 'timeline', label: '時程表', icon: 'calendar_month' },
  { name: 'stats', label: '統計', icon: 'bar_chart' },
  { name: 'trash', label: '回收桶', icon: 'delete_outline' },
  { name: 'settings', label: '設定', icon: 'tune' }
]
const navItems = computed(() => auth.isAdmin
  ? [...NAV, { name: 'admin', label: '管理', icon: 'admin_panel_settings', badge: auth.pendingCount }]
  : NAV)
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
.nav-item.mini { justify-content: center; padding: 0; }
.nav-icon { position: relative; display: flex; }
.dot {
  position: absolute; top: -3px; right: -5px; width: 10px; height: 10px; border-radius: 50%;
  background: var(--danger); border: 2px solid var(--line);
}
.collapse-btn { color: var(--sub); border-radius: 14px; min-height: 40px; margin-bottom: 4px; }
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
