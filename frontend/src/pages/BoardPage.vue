<template>
  <q-page padding>
    <div v-if="!board" class="text-grey">載入中…</div>

    <template v-else>
      <div class="row items-center q-mb-md">
        <div class="text-h5">{{ board.name }}</div>
        <q-space />
        <q-btn flat icon="add" label="新增欄位" @click="addColumn" />
      </div>

      <div class="row no-wrap q-gutter-md" style="overflow-x: auto">
        <q-card
          v-for="(c, i) in board.columns"
          :key="c.id"
          flat bordered
          class="column-card"
        >
          <q-card-section class="row items-center q-py-sm">
            <div class="text-subtitle1 ellipsis">{{ c.name }}</div>
            <q-chip v-if="c.is_done_type" dense size="sm" color="positive" text-color="white" class="q-ml-sm">完成</q-chip>
            <q-space />
            <q-btn flat dense round icon="more_vert">
              <q-menu auto-close>
                <q-list dense style="min-width: 160px">
                  <q-item clickable @click="renameColumn(c)"><q-item-section>重新命名</q-item-section></q-item>
                  <q-item clickable @click="toggleDone(c)">
                    <q-item-section>{{ c.is_done_type ? '取消完成類型' : '設為完成類型' }}</q-item-section>
                  </q-item>
                  <q-item clickable :disable="i === 0" @click="moveColumn(i, -1)"><q-item-section>左移</q-item-section></q-item>
                  <q-item clickable :disable="i === board.columns.length - 1" @click="moveColumn(i, 1)"><q-item-section>右移</q-item-section></q-item>
                  <q-separator />
                  <q-item clickable class="text-negative" :disable="board.columns.length <= 1" @click="removeColumn(c)">
                    <q-item-section>刪除</q-item-section>
                  </q-item>
                </q-list>
              </q-menu>
            </q-btn>
          </q-card-section>
          <q-separator />
          <q-card-section class="text-grey text-caption">卡片將於階段 2 實作</q-card-section>
        </q-card>
      </div>
    </template>
  </q-page>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useBoardsStore } from '../stores/boards'
import { useUiStore } from '../stores/ui'

const $q = useQuasar()
const route = useRoute()
const router = useRouter()
const boards = useBoardsStore()
const ui = useUiStore()

const board = computed(() => boards.byId(ui.currentBoardId))

// 路由 id 為準；沒有 id 或 id 不存在時導向最後開啟的看板，再不然就第一個
watch(
  () => [route.params.id, boards.loaded],
  ([id, loaded]) => {
    if (!loaded) return
    if (id && boards.byId(id)) {
      ui.setCurrentBoard(id)
      return
    }
    const fallback = boards.byId(ui.currentBoardId) || boards.boards[0]
    if (fallback) router.replace({ name: 'board', params: { id: fallback.id } })
  },
  { immediate: true }
)

// 欄位操作一律送整批陣列（後端 PUT /boards/{id}/columns）
function save (columns) {
  return boards.saveColumns(board.value.id, columns)
}

function addColumn () {
  $q.dialog({
    title: '新增欄位',
    prompt: { model: '', type: 'text', label: '名稱', maxlength: 30 },
    cancel: true
  }).onOk(name => save([...board.value.columns, { id: null, name, is_done_type: false }]))
}

function renameColumn (c) {
  $q.dialog({
    title: '重新命名',
    prompt: { model: c.name, type: 'text', label: '名稱', maxlength: 30 },
    cancel: true
  }).onOk(name => save(board.value.columns.map(x => x.id === c.id ? { ...x, name } : x)))
}

function toggleDone (c) {
  save(board.value.columns.map(x => x.id === c.id ? { ...x, is_done_type: !x.is_done_type } : x))
}

function moveColumn (i, dir) {
  const cols = [...board.value.columns]
  ;[cols[i], cols[i + dir]] = [cols[i + dir], cols[i]]
  save(cols)
}

function removeColumn (c) {
  $q.dialog({
    title: '刪除欄位',
    message: `確定刪除「${c.name}」？`,
    cancel: true,
    ok: { label: '刪除', color: 'negative' }
  }).onOk(() => save(board.value.columns.filter(x => x.id !== c.id)))
}
</script>

<style scoped>
.column-card {
  width: 280px;
  min-width: 280px;
}
</style>
