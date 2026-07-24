<template>
  <header
    class="flex items-center justify-between px-6 py-3"
    style="background: var(--color-bg-elevated); border-bottom: 1px solid var(--color-border-light); box-shadow: var(--shadow-sm);"
  >
    <div class="flex items-center gap-2 text-sm" style="color: var(--color-text-secondary);">
      <template v-if="breadcrumbs && breadcrumbs.length">
        <span
          v-for="(crumb, index) in breadcrumbs"
          :key="index"
          class="flex items-center gap-2"
        >
          <template v-if="index > 0">
            <ChevronRight style="width:14px;height:14px;color:var(--color-text-tertiary);" />
          </template>
          <span
            v-if="crumb.link"
            class="cursor-pointer font-medium"
            style="color: var(--color-primary);"
            @click="goTo(crumb.link)"
          >{{ crumb.text }}</span>
          <span v-else :class="index === breadcrumbs.length - 1 ? 'font-medium' : ''"
            :style="{ color: index === breadcrumbs.length - 1 ? 'var(--color-text-primary)' : 'var(--color-text-secondary)' }"
          >{{ crumb.text }}</span>
        </span>
      </template>
      <span v-else class="font-medium" style="color: var(--color-text-primary);">{{ pageTitle }}</span>
    </div>
    <div class="flex items-center gap-3">
      <div class="relative">
        <Bell style="width: 18px; height: 18px; color: var(--color-text-tertiary);" />
        <span
          class="absolute -top-1 -right-1 w-2 h-2 rounded-full"
          style="background: var(--state-error);"
        ></span>
      </div>
      <div
        class="flex items-center gap-2 pl-3"
        style="border-left: 1px solid var(--color-border-light);"
      >
        <div
          class="flex items-center justify-center w-8 h-8 rounded-full text-xs font-medium"
          style="background: var(--color-primary-light); color: var(--color-primary);"
        >{{ avatarText }}</div>
        <span class="text-sm font-medium" style="color: var(--color-text-primary);">{{ displayName }}</span>
        <button
          class="ml-2 px-2 py-1 text-xs rounded cursor-pointer border-0 bg-transparent"
          style="color: var(--color-text-tertiary);"
          @click="handleLogout"
          @mouseenter="(e) => { e.currentTarget.style.color = 'var(--state-error)'; }"
          @mouseleave="(e) => { e.currentTarget.style.color = 'var(--color-text-tertiary)'; }"
        >退出</button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Bell, ChevronRight } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const pageTitle = computed(() => route.meta?.title || '')
const displayName = computed(() => userStore.displayName || '管理员')

const avatarText = computed(() => {
  const name = displayName.value
  return name ? name.charAt(0) : '管'
})

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(r => r.meta && r.meta.title)
  return matched.map((r, index) => ({
    text: r.meta.title,
    link: index < matched.length - 1 ? r.path : null
  }))
})

function goTo(path) {
  router.push(path)
}

async function handleLogout() {
  await userStore.logout()
}
</script>
