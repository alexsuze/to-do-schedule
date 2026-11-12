<template>
  <!-- 公司切換（需求書 12.2）：variant = full（側邊欄）/ mini（收合）/ pill（手機標題列） -->
  <q-btn
    no-caps unelevated
    :class="['company-btn', variant]"
    :aria-label="`切換公司，目前為 ${shown.name}`"
  >
    <span class="chip" :class="{ dashed: showAll }" :style="showAll ? null : { background: shown.color }">{{ showAll ? '全' : initialOf(shown.name) }}</span>
    <template v-if="variant === 'full'">
      <span class="column col items-start" style="min-width: 0">
        <span class="text-sub" style="font-size: 12px; line-height: 1.3">目前公司</span>
        <span class="text-weight-bold ellipsis full-width text-left" style="font-size: 16px; line-height: 1.3">{{ shown.name }}</span>
      </span>
      <q-icon name="expand_more" size="22px" />
    </template>
    <template v-else-if="variant === 'pill'">
      <span class="text-weight-bold ellipsis" style="max-width: 96px">{{ shown.name }}</span>
      <q-icon name="expand_more" size="18px" />
    </template>
    <q-tooltip v-if="variant === 'mini'" anchor="center right" self="center left" :offset="[10, 0]">{{ shown.name }}（點我切換）</q-tooltip>

    <q-menu :anchor="variant === 'mini' ? 'top right' : 'bottom left'" :self="variant === 'mini' ? 'top left' : 'top left'" :offset="[0, 8]" class="company-menu">
      <div class="q-pa-sm column" style="gap: 4px; width: 272px" role="menu" aria-label="選擇公司">
        <button
          v-for="c in companies.active" :key="c.id" v-close-popup type="button" role="menuitemradio"
          :aria-checked="!showAll && c.id === companies.currentId" class="item" :class="{ on: !showAll && c.id === companies.currentId }"
          @click="companies.select(c.id)"
        >
          <span class="chip small" :style="{ background: c.color }">{{ initialOf(c.name) }}</span>
          <span class="column col" style="min-width: 0">
            <span class="text-weight-bold ellipsis" style="font-size: 15px">{{ c.name }}</span>
            <span class="text-sub" style="font-size: 12px">{{ openCount[c.id] || 0 }} 個未完成任務</span>
          </span>
          <q-icon v-if="!showAll && c.id === companies.currentId" name="check" size="20px" />
        </button>
        <button
          v-close-popup type="button" role="menuitemradio" :aria-checked="showAll" :disabled="!onTasks"
          class="item" :class="{ on: showAll, off: !onTasks }" @click="companies.select(ALL_COMPANIES)"
        >
          <span class="chip small dashed">全</span>
          <span class="column col">
            <span class="text-weight-bold" style="font-size: 15px">全部公司</span>
            <span style="font-size: 12px" :class="onTasks ? 'text-sub' : ''">{{ onTasks ? '一次看所有公司的任務' : '只有任務頁可用' }}</span>
          </span>
          <q-icon v-if="showAll" name="check" size="20px" />
        </button>
        <div class="sep" />
        <span v-if="companies.list.some(c => c.archived)" class="text-sub q-px-sm" style="font-size: 12px">已封存的公司不會出現在這裡</span>
        <button v-close-popup type="button" class="item" @click="router.push({ name: 'settings' })">
          <q-icon name="add" size="20px" class="q-mx-xs" />
          <span class="text-weight-bold" style="font-size: 14px">新增 / 管理公司（設定頁）</span>
        </button>
      </div>
    </q-menu>
  </q-btn>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCompaniesStore } from '../stores/companies'
import { useTasksStore } from '../stores/tasks'
import { ALL_COMPANIES, initialOf } from '../utils/company'

defineProps({ variant: { type: String, default: 'full' } })

const route = useRoute()
const router = useRouter()
const companies = useCompaniesStore()
const tasks = useTasksStore()

const onTasks = computed(() => route.name === 'tasks')
// 「全部公司」只在任務頁生效；其他頁顯示最後選的單一公司
const showAll = computed(() => onTasks.value && companies.all)
const shown = computed(() => showAll.value
  ? { name: '全部公司' }
  : companies.current || { name: companies.loaded ? '尚未建立公司' : '讀取中…', color: '#D9D4E0' })

const openCount = computed(() => {
  const m = {}
  for (const t of tasks.tasks) if (!t.done) m[t.company_id] = (m[t.company_id] || 0) + 1
  return m
})
</script>

<style scoped>
.company-btn {
  border: 2px solid var(--line); box-shadow: 3px 3px 0 var(--edge);
  background: var(--surface); color: var(--ink); border-radius: 14px;
}
.company-btn :deep(.q-btn__content) { flex-wrap: nowrap; gap: 10px; }
.company-btn.full { width: 100%; min-height: 56px; padding: 6px 12px; }
.company-btn.mini { width: 48px; height: 48px; min-height: 48px; padding: 0; }
.company-btn.mini .chip { border: none; width: 44px; height: 44px; border-radius: 12px; font-size: 20px; }
.company-btn.pill { border-radius: 999px; min-height: 40px; padding: 0 8px 0 4px; box-shadow: none; background: var(--soft); }
.company-btn.pill :deep(.q-btn__content) { gap: 6px; }
.company-btn.pill .chip { width: 26px; height: 26px; border-radius: 50%; font-size: 13px; }
.chip {
  width: 32px; height: 32px; flex-shrink: 0; box-sizing: border-box;
  border: 2px solid #2A2633; border-radius: 10px; color: var(--on-color);
  display: flex; align-items: center; justify-content: center;
  font-family: 'Fredoka', 'Huninn', sans-serif; font-weight: 700; font-size: 16px;
}
.chip.small { width: 28px; height: 28px; border-radius: 9px; font-size: 14px; }
.chip.dashed { border: 2px dashed var(--sub); background: transparent; color: var(--sub); font-size: 13px; }
.item {
  display: flex; align-items: center; gap: 10px; width: 100%;
  min-height: 48px; padding: 6px 10px; border: none; border-radius: 12px;
  background: transparent; color: var(--ink); text-align: left; cursor: pointer; font-family: inherit;
}
.item:hover:not(:disabled), .item.on { background: var(--soft); }
.item.off { color: var(--sub); cursor: not-allowed; }
.sep { height: 2px; background: var(--grid); margin: 4px 6px; }
</style>
<style>
.company-menu { border: 2px solid var(--line); border-radius: 18px !important; box-shadow: 5px 5px 0 var(--edge) !important; }
</style>
