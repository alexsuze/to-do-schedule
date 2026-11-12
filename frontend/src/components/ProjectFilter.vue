<template>
  <!-- 專案篩選：全部 / 各未封存專案 / 無專案；值為 'all' | 'none' | 專案 id。company 為公司 id 或 'all'（依公司分組） -->
  <q-select
    :model-value="modelValue" :options="options" emit-value map-options dense outlined
    aria-label="篩選專案" style="min-width: 160px"
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <template #option="{ itemProps, opt }">
      <q-item v-bind="itemProps">
        <q-item-section avatar><span class="cat-dot" :style="{ background: opt.color }" /></q-item-section>
        <q-item-section>{{ opt.label }}</q-item-section>
      </q-item>
    </template>
    <template #selected-item="{ opt }">
      <span class="row items-center no-wrap" style="gap: 8px">
        <span class="cat-dot" :style="{ background: opt.color }" />{{ opt.label }}
      </span>
    </template>
  </q-select>
</template>

<script setup>
import { computed } from 'vue'
import { useProjectsStore } from '../stores/projects'
import { useCompaniesStore } from '../stores/companies'
import { NO_PROJECT_COLOR } from '../utils/task'
import { ALL_COMPANIES } from '../utils/company'

const props = defineProps({
  modelValue: { type: String, default: 'all' },
  company: { type: String, default: null }
})
const emit = defineEmits(['update:modelValue'])
const projects = useProjectsStore()
const companies = useCompaniesStore()

// 全部公司時依公司排序、名稱前加公司名
const options = computed(() => {
  const all = props.company === ALL_COMPANIES
  const list = all
    ? companies.list.flatMap(c => projects.active.filter(p => p.company_id === c.id).map(p => ({ p, label: `${c.name} / ${p.name}` })))
    : projects.active.filter(p => p.company_id === props.company).map(p => ({ p, label: p.name }))
  return [
    { label: '全部專案', value: 'all', color: 'transparent' },
    ...list.map(({ p, label }) => ({ label, value: p.id, color: p.color })),
    { label: '無專案', value: 'none', color: NO_PROJECT_COLOR }
  ]
})
</script>
