<template>
  <div class="placeholder-page">
    <div class="placeholder-content">
      <Key style="width: 48px; height: 48px; color: #94A3B8;" />
      <h2>License管理</h2>
      <p>该功能正在开发中，敬请期待...</p>
    </div>
  </div>
</template>

<script setup>
import { Key } from 'lucide-vue-next'
</script>

<style scoped>
.placeholder-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 48px;
}

.placeholder-content {
  text-align: center;
}

.placeholder-content h2 {
  font-size: 20px;
  color: var(--color-text-primary);
  margin: 16px 0 8px;
}

.placeholder-content p {
  font-size: 14px;
  color: var(--color-text-tertiary);
  margin: 0;
}
</style>
