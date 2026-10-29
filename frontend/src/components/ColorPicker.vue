<template>
  <!-- 色塊按鈕，點開 4×4 色盤；目前顏色打勾。選了不同顏色才 emit pick -->
  <button type="button" class="swatch" :style="{ background: modelValue }" :aria-label="label">
    <q-menu anchor="bottom left" self="top left">
      <div class="palette">
        <button
          v-for="p in PALETTE" :key="p" v-close-popup type="button"
          class="swatch big" :style="{ background: p }"
          :aria-label="`顏色 ${p}`" :aria-pressed="String(p === modelValue)"
          @click="p !== modelValue && emit('pick', p)"
        >
          <q-icon v-if="p === modelValue" name="check" size="20px" color="dark" />
        </button>
      </div>
    </q-menu>
  </button>
</template>

<script setup>
import { PALETTE } from '../utils/task'

defineProps({
  modelValue: { type: String, required: true },
  label: { type: String, default: '改變顏色' }
})
const emit = defineEmits(['pick'])
</script>

<style scoped>
.swatch {
  width: 32px; height: 32px; flex-shrink: 0; border-radius: 10px; border: 2px solid #2A2633; cursor: pointer; padding: 0;
  display: flex; align-items: center; justify-content: center;
}
.swatch.big { width: 36px; height: 36px; }
.palette { display: grid; grid-template-columns: repeat(4, 36px); gap: 8px; padding: 12px; }
</style>
