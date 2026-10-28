<template>
  <!-- 值為本地時間字串 'YYYY-MM-DD HH:mm'，空值為 '' -->
  <q-input
    :model-value="modelValue"
    :label="label"
    mask="####-##-## ##:##"
    placeholder="YYYY-MM-DD HH:mm"
    clearable
    :hint="hint"
    @update:model-value="v => emit('update:modelValue', v || '')"
  >
    <template #append>
      <q-icon name="event" class="cursor-pointer">
        <q-popup-proxy cover transition-show="scale" transition-hide="scale">
          <q-date :model-value="modelValue || null" mask="YYYY-MM-DD HH:mm" @update:model-value="onDate">
            <div class="row items-center justify-end"><q-btn v-close-popup label="關閉" color="primary" flat no-caps /></div>
          </q-date>
        </q-popup-proxy>
      </q-icon>
      <q-icon name="access_time" class="cursor-pointer q-ml-xs">
        <q-popup-proxy cover transition-show="scale" transition-hide="scale">
          <q-time :model-value="modelValue || null" mask="YYYY-MM-DD HH:mm" format24h @update:model-value="onTime">
            <div class="row items-center justify-end"><q-btn v-close-popup label="關閉" color="primary" flat no-caps /></div>
          </q-time>
        </q-popup-proxy>
      </q-icon>
    </template>
  </q-input>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: String, default: '' },
  label: String,
  hint: String,
  defaultTime: { type: String, default: '09:00' } // 只選日期時補上的時間
})
const emit = defineEmits(['update:modelValue'])

function onDate (v) {
  if (!v) return emit('update:modelValue', '')
  // 原本沒有時間時，q-date 會補 00:00，改成預設時間
  emit('update:modelValue', props.modelValue ? v : `${v.slice(0, 10)} ${props.defaultTime}`)
}

function onTime (v) {
  if (!v) return emit('update:modelValue', '')
  // 沒有日期就先用今天
  const today = new Date()
  const pad = n => String(n).padStart(2, '0')
  const date = props.modelValue ? v.slice(0, 10) : `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`
  emit('update:modelValue', `${date} ${v.slice(11)}`)
}
</script>
