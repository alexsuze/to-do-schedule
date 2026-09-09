<template>
  <div class="row items-center q-col-gutter-sm q-mb-md">
    <q-input class="col-12 col-sm-3" v-model="ui.filters.search" dense outlined clearable
      placeholder="搜尋標題 / 描述" debounce="200">
      <template #prepend><q-icon name="search" /></template>
    </q-input>
    <q-select class="col-6 col-sm-2" v-model="ui.filters.priority" :options="PRIORITIES" label="優先度"
      dense outlined clearable emit-value map-options />
    <q-select class="col-6 col-sm-2" v-model="ui.filters.due" :options="DUE_BUCKETS" label="到期"
      dense outlined clearable emit-value map-options />
    <q-select class="col-12 col-sm-3" v-model="ui.filters.tags" :options="tagOptions" label="標籤"
      dense outlined multiple use-chips clearable />
    <div class="col-auto">
      <q-btn v-if="ui.filtering" flat dense icon="filter_alt_off" label="清除" @click="ui.clearFilters()" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUiStore } from '../stores/ui'
import { useTasksStore } from '../stores/tasks'
import { PRIORITIES, DUE_BUCKETS } from '../utils/task'

const ui = useUiStore()
const tasks = useTasksStore()

// 標籤選項來自目前看板所有事項
const tagOptions = computed(() => [...new Set(tasks.tasks.flatMap(t => t.tags || []))].sort())
</script>
