<template>
  <q-layout view="hHh Lpr lFf">
    <q-header elevated>
      <q-toolbar>
        <q-btn flat dense round icon="menu" @click="leftOpen = !leftOpen" />
        <q-toolbar-title>工作事項管理</q-toolbar-title>
      </q-toolbar>
    </q-header>

    <q-drawer v-model="leftOpen" show-if-above bordered>
      <q-list>
        <q-item-label header>看板</q-item-label>

        <q-item
          v-for="(b, i) in boards.boards"
          :key="b.id"
          clickable
          :active="b.id === ui.currentBoardId"
          active-class="bg-blue-1 text-primary"
          :to="{ name: 'board', params: { id: b.id } }"
        >
          <q-item-section avatar>
            <q-icon name="dashboard" :style="{ color: b.color || '#1976d2' }" />
          </q-item-section>
          <q-item-section>{{ b.name }}</q-item-section>
          <q-item-section side>
            <q-btn flat dense round icon="more_vert" @click.prevent.stop>
              <q-menu auto-close>
                <q-list dense style="min-width: 140px">
                  <q-item clickable @click="rename(b)"><q-item-section>重新命名</q-item-section></q-item>
                  <q-item clickable @click="changeColor(b)"><q-item-section>變更顏色</q-item-section></q-item>
                  <q-item clickable :disable="i === 0" @click="boards.move(b.id, -1)"><q-item-section>上移</q-item-section></q-item>
                  <q-item clickable :disable="i === boards.boards.length - 1" @click="boards.move(b.id, 1)"><q-item-section>下移</q-item-section></q-item>
                  <q-separator />
                  <q-item clickable class="text-negative" @click="remove(b)"><q-item-section>刪除</q-item-section></q-item>
                </q-list>
              </q-menu>
            </q-btn>
          </q-item-section>
        </q-item>

        <q-item clickable @click="create">
          <q-item-section avatar><q-icon name="add" /></q-item-section>
          <q-item-section>新增看板</q-item-section>
        </q-item>

        <q-separator class="q-my-sm" />
        <q-item clickable :to="{ name: 'trash' }" active-class="bg-blue-1 text-primary">
          <q-item-section avatar><q-icon name="delete" /></q-item-section>
          <q-item-section>回收桶</q-item-section>
        </q-item>
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useBoardsStore } from '../stores/boards'
import { useUiStore } from '../stores/ui'

const $q = useQuasar()
const router = useRouter()
const boards = useBoardsStore()
const ui = useUiStore()
const leftOpen = ref(false)

// 側邊欄辨識用色，固定幾組即可
const COLORS = [
  { label: '藍', value: '#1976d2' }, { label: '綠', value: '#21ba45' },
  { label: '橘', value: '#f2c037' }, { label: '紅', value: '#c10015' },
  { label: '紫', value: '#9c27b0' }, { label: '灰', value: '#607d8b' }
]

boards.load()

function create () {
  $q.dialog({
    title: '新增看板',
    prompt: { model: '', type: 'text', label: '名稱', maxlength: 50 },
    cancel: true
  }).onOk(async name => {
    const b = await boards.create(name, null)
    router.push({ name: 'board', params: { id: b.id } })
  })
}

function rename (b) {
  $q.dialog({
    title: '重新命名',
    prompt: { model: b.name, type: 'text', label: '名稱', maxlength: 50 },
    cancel: true
  }).onOk(name => boards.update(b.id, { name }))
}

function changeColor (b) {
  $q.dialog({
    title: '變更顏色',
    options: { type: 'radio', model: b.color || COLORS[0].value, items: COLORS },
    cancel: true
  }).onOk(color => boards.update(b.id, { color }))
}

function remove (b) {
  $q.dialog({
    title: '刪除看板',
    message: `確定刪除「${b.name}」？看板內的事項會一併移入回收桶。`,
    cancel: true,
    ok: { label: '刪除', color: 'negative' }
  }).onOk(async () => {
    await boards.remove(b.id)
    if (ui.currentBoardId === b.id) router.push({ name: 'home' })
  })
}
</script>
