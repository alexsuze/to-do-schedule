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
        <q-item disable>
          <q-item-section avatar><q-icon name="dashboard" /></q-item-section>
          <q-item-section>（尚未實作）</q-item-section>
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
const leftOpen = ref(false)
</script>
