<template>
  <q-page padding class="q-pa-xl">
    <h1 class="page-title">{{ $route.meta.title }}</h1>
    <div class="fun-card q-pa-lg q-mt-lg" style="max-width: 480px">
      <div class="text-weight-bold" style="font-size: 18px">施工中</div>
      <div class="text-sub q-mt-xs">這一頁會在階段 {{ $route.meta.stage }} 做出來，敬請期待！</div>
    </div>
  </q-page>
</template>
