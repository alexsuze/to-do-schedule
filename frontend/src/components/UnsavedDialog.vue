<template>
  <!-- 未儲存提示：回傳 'save' / 'discard'；按「繼續編輯」或關閉視窗為取消 -->
  <q-dialog ref="dialogRef" @hide="onDialogHide">
    <q-card style="width: 420px; max-width: 92vw; padding: 24px">
      <div class="font-round text-weight-bold" style="font-size: 22px">還沒儲存喔</div>
      <div class="text-sub q-mt-sm" style="font-size: 15px; line-height: 1.6">{{ message }}</div>
      <div class="row justify-end q-mt-lg" style="gap: 10px">
        <q-btn flat no-caps label="繼續編輯" @click="onDialogCancel" />
        <q-btn no-caps unelevated class="fun-btn bg-surface" label="捨棄修改" @click="onDialogOK('discard')" />
        <q-btn no-caps unelevated class="fun-btn bg-accent-fun" label="儲存修改" @click="onDialogOK('save')" />
      </div>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { useDialogPluginComponent } from 'quasar'

defineProps({ message: { type: String, default: '內容有修改還沒儲存，要怎麼處理？' } })
defineEmits([...useDialogPluginComponent.emits])
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent()
</script>
