import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/',
    component: () => import('@/views/MainLayout.vue'),
    redirect: '/projects',
    children: [
      {
        path: 'projects',
        name: 'ProjectList',
        component: () => import('@/views/ProjectList.vue'),
        meta: { roles: [], title: '项目管理' }
      },
      {
        path: 'projects/:id/config',
        name: 'ProjectConfig',
        component: () => import('@/views/ProjectConfig.vue'),
        meta: { roles: ['ADMIN', 'DEVELOPER'], title: '项目配置' }
      },
      {
        path: 'projects/:id/package',
        name: 'PackageCenter',
        component: () => import('@/views/PackageCenter.vue'),
        meta: { roles: [], title: '打包中心' }
      },
      {
        path: 'upload',
        name: 'ResourceUpload',
        component: () => import('@/views/ResourceUpload.vue'),
        meta: { roles: ['ADMIN', 'DEVELOPER'], title: '资源上传' }
      },
      {
        path: 'license',
        name: 'LicenseManagement',
        component: () => import('@/views/LicenseManagement.vue'),
        meta: { roles: ['ADMIN', 'SALES'], title: 'License管理' }
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('@/views/UserManagement.vue'),
        meta: { roles: ['ADMIN'], title: '用户管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token')

  // No auth required
  if (to.meta.noAuth) {
    if (token && to.name === 'Login') {
      return next('/projects')
    }
    return next()
  }

  // Auth required
  if (!token) {
    return next('/login')
  }

  // Dynamically import stores to avoid circular dependency
  const { useUserStore } = await import('@/stores/user')
  const userStore = useUserStore()

  // Fetch user info if not loaded
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch (e) {
      userStore.token = ''
      localStorage.removeItem('token')
      return next('/login')
    }
  }

  // Role check
  const requiredRoles = to.meta.roles
  if (requiredRoles && requiredRoles.length > 0) {
    if (!requiredRoles.includes(userStore.role)) {
      return next('/projects')
    }
  }

  next()
})

export default router
