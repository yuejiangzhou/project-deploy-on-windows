<template>
  <div class="app-sidebar flex flex-col justify-between" style="width: 220px; background: #0F172A; min-height: 100vh;">
    <div>
      <div class="flex items-center gap-2 px-5 py-4" style="border-bottom: 1px solid rgba(255,255,255,0.08);">
        <Rocket style="color: #3B82F6; width: 20px; height: 20px;" />
        <span class="font-heading text-base" style="color: #FFFFFF;">DeployManager</span>
      </div>
      <nav class="mt-2 px-3 flex flex-col gap-1">
        <router-link
          v-for="menu in permissionStore.sidebarMenus"
          :key="menu.path"
          :to="menu.path"
          class="flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium no-underline transition-colors"
          :class="isActive(menu.path) ? 'sidebar-item-active' : 'sidebar-item'"
        >
          <component :is="getIcon(menu.icon)" style="width: 16px; height: 16px;" />
          {{ menu.label }}
        </router-link>
      </nav>
    </div>
    <div class="px-5 py-4" style="border-top: 1px solid rgba(255,255,255,0.08);">
      <div class="flex items-center gap-2">
        <span class="inline-block w-2 h-2 rounded-full" style="background: #22C55E;"></span>
        <span class="text-xs" style="color: #94A3B8;">系统正常运行 v2.5.0</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { usePermissionStore } from '@/stores/permission'
import { Rocket, FolderOpen, UploadCloud, Key, Users } from 'lucide-vue-next'

const route = useRoute()
const permissionStore = usePermissionStore()

const iconMap = {
  FolderOpen,
  UploadCloud,
  Key,
  Users
}

function getIcon(iconName) {
  return iconMap[iconName] || FolderOpen
}

function isActive(path) {
  if (path === '/projects') {
    return route.path === '/projects' || route.path.startsWith('/projects/')
  }
  return route.path === path
}
</script>

<style scoped>
.sidebar-item {
  color: #94A3B8;
}
.sidebar-item:hover {
  background: rgba(148, 163, 184, 0.08);
  color: #E2E8F0;
}
.sidebar-item-active {
  background: #3B82F6;
  color: #FFFFFF;
}
</style>
