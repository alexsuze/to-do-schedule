<template>
  <!-- 專案篩選：全部 / 各未封存專案 / 無專案；值為 'all' | 'none' | 專案 id -->
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
import { NO_PROJECT_COLOR } from '../utils/task'

defineProps({ modelValue: { type: String, default: 'all' } })
const emit = defineEmits(['update:modelValue'])
const projects = useProjectsStore()

const options = computed(() => [
  { label: '全部專案', value: 'all', color: 'transparent' },
  ...projects.active.map(p => ({ label: p.name, value: p.id, color: p.color })),
  { label: '無專案', value: 'none', color: NO_PROJECT_COLOR }
])
</script>
