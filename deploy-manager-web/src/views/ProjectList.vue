<template>
  <main class="flex min-h-screen" style="background: var(--color-bg);">
    <AppSidebar />
    <div class="flex-1 flex flex-col" style="min-width: 0;">
      <AppHeader :breadcrumbs="breadcrumbList" />

      <div class="flex-1 p-6 overflow-auto">
        <!-- Page Title Row -->
        <div class="flex items-center justify-between mb-6">
          <h1 class="font-heading text-xl" style="color: var(--color-text-primary);">项目管理</h1>
          <div class="flex items-center gap-2">
            <button class="flex items-center gap-1.5 px-4 py-2 rounded-md text-sm font-medium cursor-pointer transition-colors"
                    style="border: 1px solid var(--color-border); color: var(--color-text-secondary); background: var(--color-bg-elevated);"
                    @click="openBatchDownload"
                    @mouseenter="batchBtnHover = true" @mouseleave="batchBtnHover = false"
                    :style="batchBtnHover ? { borderColor: 'var(--color-primary)', color: 'var(--color-primary)' } : {}">
              <Download style="width: 16px; height: 16px;" />
              批量下载
            </button>
            <button class="flex items-center gap-1.5 px-4 py-2 rounded-md text-sm font-medium cursor-pointer border-0"
                    style="background: var(--color-primary); color: var(--color-text-inverse);"
                    @click="showCreateModal = true"
                    @mouseenter="createBtnHover = true" @mouseleave="createBtnHover = false"
                    :style="createBtnHover ? { background: 'var(--color-primary-hover)' } : {}">
              <Plus style="width: 16px; height: 16px;" />
              新建项目
            </button>
          </div>
        </div>

        <!-- Stats Cards (2 only) -->
        <div class="grid gap-4 mb-6" style="grid-template-columns: repeat(2, 1fr);">
          <div class="rounded-lg p-4" style="background: var(--color-bg-elevated); border: 1px solid var(--color-border-light); box-shadow: var(--shadow-sm);">
            <div class="flex items-center gap-2 mb-2">
              <Folder style="width: 16px; height: 16px; color: var(--color-text-tertiary);" />
              <span class="text-sm" style="color: var(--color-text-secondary);">总项目数</span>
            </div>
            <div class="font-heading text-2xl" style="color: var(--color-primary);">{{ stats.total || 0 }}</div>
          </div>
          <div class="rounded-lg p-4" style="background: var(--color-bg-elevated); border: 1px solid var(--color-border-light); box-shadow: var(--shadow-sm);">
            <div class="flex items-center gap-2 mb-2">
              <PackageCheck style="width: 16px; height: 16px; color: var(--color-text-tertiary);" />
              <span class="text-sm" style="color: var(--color-text-secondary);">已打包数</span>
            </div>
            <div class="font-heading text-2xl" style="color: var(--state-success);">{{ stats.packaged || 0 }}</div>
          </div>
        </div>

        <!-- Search Bar -->
        <div class="flex items-center gap-3 mb-4">
          <div class="relative flex-1" style="max-width: 320px;">
            <Search style="width: 16px; height: 16px; color: var(--color-text-tertiary); position: absolute; left: 12px; top: 50%; transform: translateY(-50%);" />
            <input type="text" v-model="searchKeyword" placeholder="搜索项目..."
                   class="w-full pl-9 pr-3 py-2 rounded-md text-sm outline-none"
                   style="background: var(--color-bg-elevated); border: 1px solid var(--color-border); color: var(--color-text-primary);"
                   @focus="$event.target.style.borderColor='var(--color-primary)'"
                   @blur="$event.target.style.borderColor='var(--color-border)'"
                   @input="handleSearch">
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="text-center py-8 text-sm" style="color: var(--color-text-tertiary);">加载中...</div>

        <!-- Project Table -->
        <div v-if="!loading" class="rounded-lg overflow-hidden" style="background: var(--color-bg-elevated); border: 1px solid var(--color-border-light); box-shadow: var(--shadow-sm);">
          <table class="w-full" style="border-collapse: collapse;">
            <thead>
              <tr style="border-bottom: 1px solid var(--color-border-light);">
                <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">项目名称</th>
                <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">JAR包</th>
                <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">Vue前端</th>
                <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">状态</th>
                <th class="text-right px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in projectList" :key="p.id" class="table-row-hover" style="border-bottom: 1px solid var(--color-border-light);">
                <td class="px-4 py-3">
                  <div class="flex items-center gap-2.5">
                    <div class="flex items-center justify-center w-8 h-8 rounded-md" :style="getProjectIconBg(p.name)">
                      <component :is="getProjectIcon(p.name)" style="width: 16px; height: 16px;" :style="{ color: getProjectIconColor(p.name) }" />
                    </div>
                    <div>
                      <div class="text-sm font-medium" style="color: var(--color-text-primary);">{{ p.name }}</div>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-3">
                  <span v-if="p.jarFileName" class="text-sm font-mono" style="color: var(--color-text-secondary);">{{ p.jarFileName }}</span>
                  <span v-else class="text-sm font-mono" style="color: var(--color-text-tertiary);">未上传</span>
                </td>
                <td class="px-4 py-3">
                  <span v-if="p.vueConfigured" class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium" style="background: var(--state-success-bg); color: var(--state-success);">已配置</span>
                  <span v-else class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium" style="background: var(--color-bg-sunken); color: var(--color-text-tertiary);">未配置</span>
                </td>
                <td class="px-4 py-3">
                  <span :class="getStatusClass(p.status)" class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium">
                    {{ getStatusText(p.status) }}
                  </span>
                </td>
                <td class="px-4 py-3 text-right">
                  <div class="flex items-center justify-end gap-2">
                    <button class="px-3 py-1 rounded text-xs font-medium cursor-pointer border-0"
                            style="background: var(--color-primary); color: var(--color-text-inverse);"
                            @click="goToConfig(p.id)">
                      配置
                    </button>
                    <button class="px-3 py-1 rounded text-xs font-medium cursor-pointer border-0"
                            style="background: var(--state-success); color: var(--color-text-inverse);"
                            @click="goToPackage(p.id)">
                      打包
                    </button>
                    <button class="flex items-center gap-1.5 px-3 py-1 rounded text-xs font-medium cursor-pointer transition-colors"
                            style="border: 1px solid var(--color-border); color: var(--color-text-secondary); background: var(--color-bg-elevated);"
                            @mouseenter="(e) => { e.currentTarget.style.borderColor='var(--color-primary)'; e.currentTarget.style.color='var(--color-primary)'; }"
                            @mouseleave="(e) => { e.currentTarget.style.borderColor='var(--color-border)'; e.currentTarget.style.color='var(--color-text-secondary)'; }"
                            @click="downloadLatest(p)">
                      <Download style="width: 13px; height: 13px;" />
                      <span>下载最新版本</span>
                    </button>
                    <button class="px-3 py-1 rounded text-xs font-medium cursor-pointer border-0"
                            style="background: var(--color-bg-sunken); color: var(--color-text-secondary);"
                            @click="handleDelete(p)">
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Table Footer Info -->
        <div class="flex items-center justify-between mt-4 px-1">
          <span class="text-xs" style="color: var(--color-text-tertiary);">共 {{ total }} 条记录 / {{ totalPages }} 页</span>
          <div class="flex items-center gap-1">
            <button class="px-2.5 py-1 rounded text-xs cursor-pointer border-0"
                    :style="page <= 1
                      ? { background: 'var(--color-bg-sunken)', color: 'var(--color-text-tertiary)', opacity: 0.5, cursor: 'not-allowed' }
                      : { background: 'var(--color-bg-sunken)', color: 'var(--color-text-tertiary)' }"
                    :disabled="page <= 1"
                    @click="changePage(page - 1)">
              上一页
            </button>
            <template v-for="p in pageNumbers" :key="'page-' + p">
              <span v-if="p === '...'" class="px-2 text-xs" style="color: var(--color-text-tertiary);">...</span>
              <button v-else
                      class="px-2.5 py-1 rounded text-xs cursor-pointer border-0"
                      :style="p === page
                        ? { background: 'var(--color-primary)', color: 'var(--color-text-inverse)' }
                        : { background: 'var(--color-bg-sunken)', color: 'var(--color-text-tertiary)' }"
                      @click="changePage(p)">
                {{ p }}
              </button>
            </template>
            <button class="px-2.5 py-1 rounded text-xs cursor-pointer border-0"
                    :style="page >= totalPages
                      ? { background: 'var(--color-bg-sunken)', color: 'var(--color-text-tertiary)', opacity: 0.5, cursor: 'not-allowed' }
                      : { background: 'var(--color-bg-sunken)', color: 'var(--color-text-tertiary)' }"
                    :disabled="page >= totalPages"
                    @click="changePage(page + 1)">
              下一页
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建项目 Modal -->
    <div v-show="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
      <div class="modal-card" style="width: 400px;">
        <h2 class="font-heading text-lg" style="color: var(--color-text-primary); margin-bottom: 24px;">新建项目</h2>
        <div style="margin-bottom: 16px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">项目名称</label>
          <input type="text" v-model="newProject.name" placeholder="请输入项目名称"
                 class="w-full box-border rounded-md text-sm outline-none"
                 style="padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-primary);"
                 @focus="$event.target.style.borderColor='var(--color-primary)'"
                 @blur="$event.target.style.borderColor='var(--color-border)'">
        </div>
        <div style="margin-bottom: 24px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">项目描述</label>
          <textarea v-model="newProject.description" placeholder="简要描述项目用途，可选" rows="2"
                    class="w-full box-border rounded-md text-sm outline-none"
                    style="padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-primary); resize: vertical;"
                    @focus="$event.target.style.borderColor='var(--color-primary)'"
                    @blur="$event.target.style.borderColor='var(--color-border)'"></textarea>
        </div>
        <div style="display: flex; justify-content: flex-end; gap: 8px;">
          <button class="rounded-md text-sm cursor-pointer"
                  style="padding: 6px 16px; border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-secondary);"
                  @click="showCreateModal = false">
            取消
          </button>
          <button class="rounded-md text-sm cursor-pointer border-0"
                  style="padding: 6px 16px; background: var(--color-primary); color: var(--color-text-inverse);"
                  @click="createProject">
            创建
          </button>
        </div>
      </div>
    </div>

    <!-- 批量下载 Modal -->
    <div v-show="showBatchModal" class="modal-overlay" @click.self="showBatchModal = false">
      <div class="modal-card" style="width: 500px; max-height: 80vh; display: flex; flex-direction: column;">
        <h2 class="font-heading text-lg" style="color: var(--color-text-primary); margin-bottom: 16px;">批量下载</h2>
        <p class="text-xs mb-4" style="color: var(--color-text-tertiary);">选择需要下载最新打包产物的项目（仅已打包的项目可下载）</p>
        <div style="flex: 1; overflow-y: auto; margin-bottom: 16px;">
          <div v-if="batchLoading" style="text-align: center; padding: 20px; color: var(--color-text-tertiary);">加载中...</div>
          <div v-else-if="batchProjects.length === 0" style="text-align: center; padding: 20px; color: var(--color-text-tertiary);">暂无项目</div>
          <div v-for="p in batchProjects" :key="'batch-' + p.id"
               class="flex items-center gap-3 px-3 py-2.5 rounded-md cursor-pointer"
               style="margin-bottom: 4px; border: 1px solid var(--color-border-light);"
               :style="batchSelected.includes(p.id) ? { borderColor: 'var(--color-primary)', background: 'var(--color-primary-light)' } : {}"
               @click="toggleBatchSelect(p.id)">
            <input type="checkbox" :checked="batchSelected.includes(p.id)" @click.stop="toggleBatchSelect(p.id)" style="cursor: pointer;">
            <Folder style="width: 16px; height: 16px; color: var(--color-text-tertiary);" />
            <span class="text-sm flex-1" style="color: var(--color-text-primary);">{{ p.name }}</span>
            <span v-if="p.hasPackage" class="text-xs px-2 py-0.5 rounded-full" style="background: var(--state-success-bg); color: var(--state-success);">有包</span>
            <span v-else class="text-xs px-2 py-0.5 rounded-full" style="background: var(--color-bg-sunken); color: var(--color-text-tertiary);">无包</span>
          </div>
        </div>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <button class="text-sm cursor-pointer border-0 bg-transparent"
                  style="color: var(--color-primary);"
                  @click="toggleSelectAll">
            {{ batchSelected.length === batchProjects.filter(p => p.hasPackage).length && batchProjects.filter(p => p.hasPackage).length > 0 ? '取消全选' : '全选有包项目' }}
          </button>
          <div style="display: flex; gap: 8px;">
            <button class="rounded-md text-sm cursor-pointer"
                    style="padding: 6px 16px; border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-secondary);"
                    @click="showBatchModal = false">
              取消
            </button>
            <button class="rounded-md text-sm cursor-pointer border-0"
                    style="padding: 6px 16px; background: var(--color-primary); color: var(--color-text-inverse);"
                    :disabled="batchSelected.length === 0"
                    :style="batchSelected.length === 0 ? { opacity: 0.5, cursor: 'not-allowed' } : {}"
                    @click="executeBatchDownload">
              下载选中 ({{ batchSelected.length }})
            </button>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Folder, PackageCheck, Search, Download, Plus,
  ShoppingCart, Database, Users, FileCheck
} from 'lucide-vue-next'
import AppSidebar from '@/components/AppSidebar.vue'
import AppHeader from '@/components/AppHeader.vue'
import { getProjectList, getProjectStats, createProject as createProjectApi, deleteProject, downloadLatestPackageUrl, getLatestPackage } from '@/api/project'

const router = useRouter()

const breadcrumbList = computed(() => [
  { text: '项目管理' }
])

const batchBtnHover = ref(false)
const createBtnHover = ref(false)
const showCreateModal = ref(false)
const showBatchModal = ref(false)
const batchLoading = ref(false)
const batchProjects = ref([])
const batchSelected = ref([])
const searchKeyword = ref('')
const page = ref(1)
const pageSize = 10
const total = ref(0)
const projectList = ref([])
const stats = reactive({ total: 0, packaged: 0 })
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

const loading = ref(false)
const searchTimeout = ref(null)

const pageNumbers = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = page.value
  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    pages.push(1)
    if (current > 3) pages.push('...')
    const start = Math.max(2, current - 1)
    const end = Math.min(total - 1, current + 1)
    for (let i = start; i <= end; i++) pages.push(i)
    if (current < total - 2) pages.push('...')
    pages.push(total)
  }
  return pages
})

const newProject = reactive({
  name: '',
  description: ''
})

function getProjectIcon(name) {
  if (name.includes('电商') || name.includes('购物') || name.includes('商城')) return ShoppingCart
  if (name.includes('数据') || name.includes('中台')) return Database
  if (name.includes('用户') || name.includes('会员')) return Users
  if (name.includes('审批') || name.includes('流程')) return FileCheck
  return Folder
}

function getProjectIconBg(name) {
  if (name.includes('电商') || name.includes('购物') || name.includes('商城')) return 'background: #EFF6FF;'
  if (name.includes('数据') || name.includes('中台')) return 'background: #F0FDF4;'
  if (name.includes('用户') || name.includes('会员')) return 'background: #EFF6FF;'
  if (name.includes('审批') || name.includes('流程')) return 'background: var(--color-bg-sunken);'
  return 'background: var(--color-bg-sunken);'
}

function getProjectIconColor(name) {
  if (name.includes('电商') || name.includes('购物') || name.includes('商城')) return 'var(--color-primary);'
  if (name.includes('数据') || name.includes('中台')) return 'var(--state-success);'
  if (name.includes('用户') || name.includes('会员')) return 'var(--color-primary);'
  if (name.includes('审批') || name.includes('流程')) return 'var(--color-text-tertiary);'
  return 'var(--color-text-tertiary);'
}

function getStatusText(status) {
  const map = {
    READY: '已就绪',
    CONFIGURING: '配置中',
    ARCHIVED: '已归档',
    DRAFT: '草稿'
  }
  return map[status] || status || '草稿'
}

function getStatusClass(status) {
  const map = {
    READY: 'background: var(--state-success-bg); color: var(--state-success);',
    CONFIGURING: 'background: var(--state-info-bg); color: var(--state-info);',
    ARCHIVED: 'background: var(--color-bg-sunken); color: var(--color-text-tertiary);',
    DRAFT: 'background: var(--color-bg-sunken); color: var(--color-text-tertiary);'
  }
  return map[status] || map.DRAFT
}

async function loadProjects() {
  loading.value = true
  try {
    const res = await getProjectList({ page: page.value, pageSize: pageSize, keyword: searchKeyword.value })
    projectList.value = res?.records || res?.list || res || []
    total.value = res?.total || projectList.value.length
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await getProjectStats()
    stats.total = res?.total || 0
    stats.packaged = res?.packaged || 0
  } catch (e) {
    console.error(e)
  }
}

function handleSearch() {
  if (searchTimeout.value) clearTimeout(searchTimeout.value)
  searchTimeout.value = setTimeout(() => {
    page.value = 1
    loadProjects()
  }, 300)
}

function changePage(p) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  loadProjects()
}

function goToConfig(id) {
  router.push(`/projects/${id}/config`)
}

function goToPackage(id) {
  router.push(`/projects/${id}/package`)
}

async function downloadLatest(p) {
  try {
    const res = await getLatestPackage(p.id)
    const pkg = res?.data || res
    if (pkg && pkg.id) {
      const url = downloadLatestPackageUrl(p.id)
      window.open(url, '_blank')
    } else {
      await ElMessageBox.confirm(
        `项目「${p.name}」暂无可下载的包，请先完成配置并打包。`,
        '提示',
        {
          confirmButtonText: '前往配置',
          cancelButtonText: '我知道了',
          type: 'info'
        }
      ).then(() => {
        router.push(`/projects/${p.id}/config`)
      }).catch(() => {})
    }
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    console.error(e)
    ElMessage.error('查询最新包失败')
  }
}

async function openBatchDownload() {
  showBatchModal.value = true
  batchLoading.value = true
  batchSelected.value = []
  try {
    const res = await getProjectList({ page: 1, pageSize: 9999 })
    const list = res?.records || res?.list || res || []
    const checked = await Promise.all(list.map(async (p) => {
      try {
        const pkgRes = await getLatestPackage(p.id)
        const pkg = pkgRes?.data || pkgRes
        return { ...p, hasPackage: !!(pkg && pkg.id) }
      } catch (e) {
        return { ...p, hasPackage: false }
      }
    }))
    batchProjects.value = checked
  } catch (e) {
    console.error(e)
    ElMessage.error('加载项目列表失败')
  } finally {
    batchLoading.value = false
  }
}

function toggleBatchSelect(id) {
  const idx = batchSelected.value.indexOf(id)
  if (idx >= 0) {
    batchSelected.value.splice(idx, 1)
  } else {
    batchSelected.value.push(id)
  }
}

function toggleSelectAll() {
  const packagedProjects = batchProjects.value.filter(p => p.hasPackage)
  if (batchSelected.value.length === packagedProjects.length && packagedProjects.length > 0) {
    batchSelected.value = []
  } else {
    batchSelected.value = packagedProjects.map(p => p.id)
  }
}

async function executeBatchDownload() {
  if (batchSelected.value.length === 0) return
  let successCount = 0
  let failCount = 0
  for (const id of batchSelected.value) {
    const project = batchProjects.value.find(p => p.id === id)
    if (!project || !project.hasPackage) {
      failCount++
      continue
    }
    try {
      const url = downloadLatestPackageUrl(id)
      const iframe = document.createElement('iframe')
      iframe.style.display = 'none'
      iframe.src = url
      document.body.appendChild(iframe)
      setTimeout(() => {
        document.body.removeChild(iframe)
      }, 60000)
      successCount++
    } catch (e) {
      console.error(e)
      failCount++
    }
  }
  if (successCount > 0) {
    ElMessage.success(`已开始下载 ${successCount} 个项目的最新包${failCount > 0 ? `，${failCount} 个失败` : ''}`)
  } else {
    ElMessage.warning('没有可下载的包')
  }
  showBatchModal.value = false
}

async function createProject() {
  if (!newProject.name.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  try {
    await createProjectApi({ name: newProject.name, description: newProject.description })
    ElMessage.success('创建成功')
    showCreateModal.value = false
    newProject.name = ''
    newProject.description = ''
    loadProjects()
    loadStats()
  } catch (e) {
    console.error(e)
  }
}

async function handleDelete(p) {
  try {
    await ElMessageBox.confirm(`确认删除项目「${p.name}」？`, '提示', { type: 'warning' })
    await deleteProject(p.id)
    ElMessage.success('删除成功')
    loadProjects()
    loadStats()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

onMounted(() => {
  loadProjects()
  loadStats()
})
</script>

<style scoped>
.table-row-hover:hover { background: var(--color-bg-sunken); }
.table-row-hover { transition: background 150ms ease; }

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-card {
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-float);
  padding: var(--space-lg);
}
</style>
