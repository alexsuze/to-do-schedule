<template>
  <!-- 看板 / 欄位共用的選色視窗：色階網格為主，可切到光譜自由選 -->
  <q-dialog v-model="open" @hide="$emit('close')">
    <q-card style="width: 420px; max-width: 95vw">
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">{{ title }}</div>
        <q-space />
        <q-btn flat round dense icon="close" v-close-popup />
      </q-card-section>
      <q-card-section>
        <q-color v-model="color" no-header default-view="palette" :palette="PALETTE" format-model="hex" class="full-width" />
      </q-card-section>
      <q-card-actions align="between">
        <q-btn flat label="清除顏色" @click="pick(null)" />
        <div>
          <q-btn flat label="取消" v-close-popup />
          <q-btn color="primary" label="確定" @click="pick(color)" />
        </div>
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: null }, // 目前顏色，null 表示未開啟
  title: { type: String, default: '變更顏色' },
  show: Boolean
})
const emit = defineEmits(['pick', 'close'])

const open = ref(false)
const color = ref('#1976d2')
watch(() => props.show, v => {
  if (v) color.value = props.modelValue || '#1976d2'
  open.value = v
}, { immediate: true })

function pick (c) {
  emit('pick', c)
  open.value = false
}

// 12 色相 × 5 深淺 + 灰階，HSL 轉 hex
function hsl (h, s, l) {
  const a = s * Math.min(l, 1 - l)
  const f = n => {
    const k = (n + h / 30) % 12
    const c = l - a * Math.max(-1, Math.min(k - 3, 9 - k, 1))
    return Math.round(255 * c).toString(16).padStart(2, '0')
  }
  return `#${f(0)}${f(8)}${f(4)}`
}
const PALETTE = []
for (const l of [0.82, 0.68, 0.52, 0.4, 0.28]) {
  for (let h = 0; h < 360; h += 30) PALETTE.push(hsl(h, 0.65, l))
}
for (const l of [0.9, 0.75, 0.6, 0.45, 0.3, 0.15]) PALETTE.push(hsl(0, 0, l))
</script>
