<template>
  <main class="flex min-h-screen" style="background: var(--color-bg);">
    <AppSidebar />
    <div class="flex-1 flex flex-col" style="min-width: 0;">
      <AppHeader />
      <div class="flex-1 overflow-auto">
        <router-view />
      </div>
    </div>
  </main>
</template>

<script setup>
import AppSidebar from '@/components/AppSidebar.vue'
import AppHeader from '@/components/AppHeader.vue'
</script>
