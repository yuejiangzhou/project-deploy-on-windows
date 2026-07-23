import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/projects' },
  {
    path: '/projects',
    name: 'ProjectList',
    component: () => import('@/views/ProjectList.vue'),
    meta: { sidebarActive: 'projects' }
  },
  {
    path: '/projects/:id/config',
    name: 'ProjectConfig',
    component: () => import('@/views/ProjectConfig.vue'),
    meta: { sidebarActive: null }
  },
  {
    path: '/upload',
    name: 'ResourceUpload',
    component: () => import('@/views/ResourceUpload.vue'),
    meta: { sidebarActive: 'upload' }
  },
  {
    path: '/projects/:id/package',
    name: 'PackageCenter',
    component: () => import('@/views/PackageCenter.vue'),
    meta: { sidebarActive: null }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
