<template>
  <!-- 任務的「專案 / 分類」標籤：有分類用分類色，否則用專案色；showCompany 時前面加公司名 -->
  <span class="cat-chip ellipsis" :style="{ background: tag.color, maxWidth: '100%' }" :title="tag.text">{{ tag.text }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { useProjectsStore } from '../stores/projects'
import { useCategoriesStore } from '../stores/categories'
import { useCompaniesStore } from '../stores/companies'

const props = defineProps({ task: { type: Object, required: true }, showCompany: Boolean })
const projects = useProjectsStore()
const cats = useCategoriesStore()
const companies = useCompaniesStore()

const tag = computed(() => {
  const p = projects.label(props.task.project_id)
  const c = props.task.category_id && cats.byId(props.task.category_id)
  const t = c ? { text: `${p.name} / ${c.name}`, color: c.color } : { text: p.name, color: p.color }
  return props.showCompany ? { ...t, text: `${companies.label(props.task.company_id).name} / ${t.text}` } : t
})
</script>
