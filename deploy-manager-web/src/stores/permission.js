import { defineStore } from 'pinia'
import { computed } from 'vue'
import { useUserStore } from './user'

export const usePermissionStore = defineStore('permission', () => {
  const userStore = useUserStore()

  const sidebarMenus = computed(() => {
    const role = userStore.role
    const menus = []

    // All roles
    menus.push({
      path: '/projects',
      label: '项目管理',
      icon: 'FolderOpen'
    })

    // ADMIN + DEVELOPER
    if (role === 'ADMIN' || role === 'DEVELOPER') {
      menus.push({
        path: '/upload',
        label: '资源上传',
        icon: 'UploadCloud'
      })
    }

    // ADMIN + SALES
    if (role === 'ADMIN' || role === 'SALES') {
      menus.push({
        path: '/license',
        label: 'License管理',
        icon: 'Key'
      })
    }

    // ADMIN only
    if (role === 'ADMIN') {
      menus.push({
        path: '/users',
        label: '用户管理',
        icon: 'Users'
      })
    }

    return menus
  })

  function hasAccess(requiredRoles) {
    if (!requiredRoles || requiredRoles.length === 0) return true
    return requiredRoles.includes(userStore.role)
  }

  return {
    sidebarMenus,
    hasAccess
  }
})
