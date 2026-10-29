<template>
  <!-- 任務的「專案 / 分類」標籤：有分類用分類色，否則用專案色 -->
  <span class="cat-chip ellipsis" :style="{ background: tag.color, maxWidth: '100%' }" :title="tag.text">{{ tag.text }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { useProjectsStore } from '../stores/projects'
import { useCategoriesStore } from '../stores/categories'

const props = defineProps({ task: { type: Object, required: true } })
const projects = useProjectsStore()
const cats = useCategoriesStore()

const tag = computed(() => {
  const p = projects.label(props.task.project_id)
  const c = props.task.category_id && cats.byId(props.task.category_id)
  return c ? { text: `${p.name} / ${c.name}`, color: c.color } : { text: p.name, color: p.color }
})
</script>
