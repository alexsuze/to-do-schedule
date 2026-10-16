<template>
  <q-page padding>
    <div class="row items-center q-mb-md">
      <div class="text-h5">總時程表</div>
      <q-badge color="grey-5" class="q-ml-sm">{{ visible.length }}</q-badge>
    </div>

    <FilterBar />
    <TimelineView :boards="boards.boards" :tasks="visible" show-board @edit="editing = $event" />

    <TaskDialog :task="editing" @close="editing = null" @open="editing = $event" />
  </q-page>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useBoardsStore } from '../stores/boards'
import { useTasksStore } from '../stores/tasks'
import { useUiStore } from '../stores/ui'
import { filterTasks } from '../utils/task'
import FilterBar from '../components/FilterBar.vue'
import TimelineView from '../components/TimelineView.vue'
import TaskDialog from '../components/TaskDialog.vue'

const boards = useBoardsStore()
const tasks = useTasksStore()
const ui = useUiStore()
const editing = ref(null)

tasks.loadAll()
const visible = computed(() => filterTasks(tasks.tasks, ui.filters))
</script>
