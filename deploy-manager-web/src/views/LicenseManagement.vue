<template>
  <div class="p-6">
    <!-- Page Title Row -->
    <div class="flex items-center justify-between mb-6">
      <h1 class="font-heading text-xl" style="color: var(--color-text-primary);">License管理</h1>
      <button class="flex items-center gap-1.5 px-4 py-2 rounded-md text-sm font-medium cursor-pointer border-0"
              style="background: var(--color-primary); color: var(--color-text-inverse);"
              @click="openGenerateModal"
              @mouseenter="generateBtnHover = true" @mouseleave="generateBtnHover = false"
              :style="generateBtnHover ? { background: 'var(--color-primary-hover)' } : {}">
        <Plus style="width: 16px; height: 16px;" />
        生成 License
      </button>
    </div>

    <!-- Search Bar -->
    <div class="flex items-center gap-3 mb-4">
      <div class="flex items-center gap-2">
        <label class="text-sm" style="color: var(--color-text-secondary);">项目:</label>
        <select v-model="searchProjectId"
                class="px-3 py-2 rounded-md text-sm outline-none cursor-pointer"
                style="background: var(--color-bg-elevated); border: 1px solid var(--color-border); color: var(--color-text-primary); min-width: 160px;"
                @change="handleSearch">
          <option value="">全部项目</option>
          <option v-for="p in projectList" :key="p.id" :value="p.id">{{ p.name }}</option>
        </select>
      </div>
      <div class="flex items-center gap-2">
        <label class="text-sm" style="color: var(--color-text-secondary);">状态:</label>
        <select v-model="searchStatus"
                class="px-3 py-2 rounded-md text-sm outline-none cursor-pointer"
                style="background: var(--color-bg-elevated); border: 1px solid var(--color-border); color: var(--color-text-primary); min-width: 120px;"
                @change="handleSearch">
          <option value="">全部</option>
          <option value="ACTIVE">ACTIVE</option>
          <option value="EXPIRED">EXPIRED</option>
          <option value="REVOKED">REVOKED</option>
        </select>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center py-8 text-sm" style="color: var(--color-text-tertiary);">加载中...</div>

    <!-- License Table -->
    <div v-if="!loading" class="rounded-lg overflow-hidden" style="background: var(--color-bg-elevated); border: 1px solid var(--color-border-light); box-shadow: var(--shadow-sm);">
      <table class="w-full" style="border-collapse: collapse;">
        <thead>
          <tr style="border-bottom: 1px solid var(--color-border-light);">
            <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">客户名称</th>
            <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">关联项目</th>
            <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">类型</th>
            <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">试用天数</th>
            <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">过期时间</th>
            <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">状态</th>
            <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">生成时间</th>
            <th class="text-right px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in licenseList" :key="item.id" class="table-row-hover" style="border-bottom: 1px solid var(--color-border-light);">
            <td class="px-4 py-3">
              <span class="text-sm font-medium" style="color: var(--color-text-primary);">{{ item.customerName }}</span>
            </td>
            <td class="px-4 py-3">
              <span class="text-sm" style="color: var(--color-text-secondary);">{{ item.projectName || '-' }}</span>
            </td>
            <td class="px-4 py-3">
              <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
                    :style="item.type === 'PERMANENT'
                      ? { background: 'var(--color-primary-light)', color: 'var(--color-primary)' }
                      : { background: 'var(--state-info-bg)', color: 'var(--state-info)' }">
                {{ item.type === 'PERMANENT' ? '永久' : '试用' }}
              </span>
            </td>
            <td class="px-4 py-3">
              <span class="text-sm" style="color: var(--color-text-secondary);">
                {{ item.type === 'PERMANENT' ? '-' : (item.trialDays || '-') }}
              </span>
            </td>
            <td class="px-4 py-3">
              <span class="text-sm" style="color: var(--color-text-secondary);">{{ formatDate(item.expireTime) }}</span>
            </td>
            <td class="px-4 py-3">
              <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
                    :style="getStatusStyle(item.status)">
                {{ getStatusText(item.status) }}
              </span>
            </td>
            <td class="px-4 py-3">
              <span class="text-sm" style="color: var(--color-text-secondary);">{{ formatDate(item.createdAt) }}</span>
            </td>
            <td class="px-4 py-3 text-right">
              <div class="flex items-center justify-end gap-2">
                <button class="px-3 py-1 rounded text-xs font-medium cursor-pointer border-0"
                        style="background: var(--color-primary); color: var(--color-text-inverse);"
                        @click="handleDownload(item)">
                  下载
                </button>
                <button v-if="item.status === 'ACTIVE'"
                        class="px-3 py-1 rounded text-xs font-medium cursor-pointer"
                        style="border: 1px solid var(--color-border); color: var(--color-text-secondary); background: var(--color-bg-elevated);"
                        @click="openRenewModal(item)">
                  续期
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="licenseList.length === 0">
            <td colspan="8" class="px-4 py-8 text-center text-sm" style="color: var(--color-text-tertiary);">暂无License记录</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
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

    <!-- Generate License Modal -->
    <div v-show="showGenerateModal" class="modal-overlay" @click.self="showGenerateModal = false">
      <div class="modal-card" style="width: 440px;">
        <h2 class="font-heading text-lg" style="color: var(--color-text-primary); margin-bottom: 24px;">生成 License</h2>
        <div style="margin-bottom: 16px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">选择项目 <span style="color: var(--color-danger);">*</span></label>
          <select v-model="generateForm.projectId"
                  class="w-full box-border rounded-md text-sm outline-none cursor-pointer"
                  style="padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-primary); background: var(--color-bg-elevated);">
            <option value="">请选择项目</option>
            <option v-for="p in projectList" :key="p.id" :value="p.id">{{ p.name }}</option>
          </select>
        </div>
        <div style="margin-bottom: 16px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">客户名称 <span style="color: var(--color-danger);">*</span></label>
          <input type="text" v-model="generateForm.customerName" placeholder="请输入客户名称"
                 class="w-full box-border rounded-md text-sm outline-none"
                 style="padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-primary);"
                 @focus="$event.target.style.borderColor='var(--color-primary)'"
                 @blur="$event.target.style.borderColor='var(--color-border)'">
        </div>
        <div style="margin-bottom: 16px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">试用天数</label>
          <input type="number" v-model.number="generateForm.trialDays" min="1"
                 class="w-full box-border rounded-md text-sm outline-none"
                 style="padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-primary); width: 120px;"
                 @focus="$event.target.style.borderColor='var(--color-primary)'"
                 @blur="$event.target.style.borderColor='var(--color-border)'">
        </div>
        <div style="margin-bottom: 24px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">类型</label>
          <div class="flex items-center gap-6" style="padding: 8px 0;">
            <label class="flex items-center gap-2 cursor-pointer text-sm" style="color: var(--color-text-primary);">
              <input type="radio" v-model="generateForm.type" value="TRIAL" style="accent-color: var(--color-primary);">
              试用 (TRIAL)
            </label>
            <label class="flex items-center gap-2 cursor-pointer text-sm" style="color: var(--color-text-primary);">
              <input type="radio" v-model="generateForm.type" value="PERMANENT" style="accent-color: var(--color-primary);">
              永久 (PERMANENT)
            </label>
          </div>
        </div>
        <div style="display: flex; justify-content: flex-end; gap: 8px;">
          <button class="rounded-md text-sm cursor-pointer"
                  style="padding: 6px 16px; border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-secondary);"
                  @click="showGenerateModal = false">
            取消
          </button>
          <button class="rounded-md text-sm cursor-pointer border-0"
                  style="padding: 6px 16px; background: var(--color-primary); color: var(--color-text-inverse);"
                  :disabled="generateLoading"
                  @click="handleGenerate">
            {{ generateLoading ? '生成中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Renew Modal -->
    <div v-show="showRenewModal" class="modal-overlay" @click.self="showRenewModal = false">
      <div class="modal-card" style="width: 400px;">
        <h2 class="font-heading text-lg" style="color: var(--color-text-primary); margin-bottom: 24px;">续期 License</h2>
        <p class="text-sm mb-4" style="color: var(--color-text-secondary);">客户: {{ renewTarget?.customerName }} | 项目: {{ renewTarget?.projectName }}</p>
        <div style="margin-bottom: 24px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">新试用天数 <span style="color: var(--color-danger);">*</span></label>
          <input type="number" v-model.number="renewDays" min="1"
                 class="w-full box-border rounded-md text-sm outline-none"
                 style="padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-primary); width: 120px;"
                 @focus="$event.target.style.borderColor='var(--color-primary)'"
                 @blur="$event.target.style.borderColor='var(--color-border)'">
        </div>
        <div style="display: flex; justify-content: flex-end; gap: 8px;">
          <button class="rounded-md text-sm cursor-pointer"
                  style="padding: 6px 16px; border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-secondary);"
                  @click="showRenewModal = false">
            取消
          </button>
          <button class="rounded-md text-sm cursor-pointer border-0"
                  style="padding: 6px 16px; background: var(--color-primary); color: var(--color-text-inverse);"
                  :disabled="renewLoading"
                  @click="handleRenew">
            {{ renewLoading ? '续期中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Download } from 'lucide-vue-next'
import { getLicenseList, generateLicense, renewLicense, getDownloadUrl } from '@/api/license'
import { getProjectList } from '@/api/project'
import { formatDate as fmtDate } from '@/utils/format'

const generateBtnHover = ref(false)
const loading = ref(false)
const generateLoading = ref(false)
const renewLoading = ref(false)
const showGenerateModal = ref(false)
const showRenewModal = ref(false)

const searchProjectId = ref('')
const searchStatus = ref('')
const page = ref(1)
const pageSize = 10
const total = ref(0)
const licenseList = ref([])
const projectList = ref([])
const renewTarget = ref(null)
const renewDays = ref(30)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

const pageNumbers = computed(() => {
  const pages = []
  const t = totalPages.value
  const current = page.value
  if (t <= 7) {
    for (let i = 1; i <= t; i++) pages.push(i)
  } else {
    pages.push(1)
    if (current > 3) pages.push('...')
    const start = Math.max(2, current - 1)
    const end = Math.min(t - 1, current + 1)
    for (let i = start; i <= end; i++) pages.push(i)
    if (current < t - 2) pages.push('...')
    pages.push(t)
  }
  return pages
})

const generateForm = reactive({
  projectId: '',
  customerName: '',
  trialDays: 30,
  type: 'TRIAL'
})

function formatDate(dateStr) {
  return fmtDate(dateStr)
}

function getStatusText(status) {
  const map = { ACTIVE: '有效', EXPIRED: '已过期', REVOKED: '已撤销' }
  return map[status] || status || '-'
}

function getStatusStyle(status) {
  const map = {
    ACTIVE: { background: 'var(--state-success-bg)', color: 'var(--state-success)' },
    EXPIRED: { background: 'var(--color-bg-sunken)', color: 'var(--color-text-tertiary)' },
    REVOKED: { background: 'var(--state-error-bg)', color: 'var(--state-error)' }
  }
  return map[status] || {}
}

async function loadProjects() {
  try {
    const res = await getProjectList({ page: 1, pageSize: 9999 })
    projectList.value = res?.records || res?.list || res || []
  } catch (e) {
    console.error(e)
  }
}

async function loadLicenses() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize }
    if (searchProjectId.value) params.projectId = searchProjectId.value
    if (searchStatus.value) params.status = searchStatus.value
    const res = await getLicenseList(params)
    licenseList.value = res?.records || res || []
    total.value = res?.total || licenseList.value.length
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadLicenses()
}

function changePage(p) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  loadLicenses()
}

function openGenerateModal() {
  generateForm.projectId = ''
  generateForm.customerName = ''
  generateForm.trialDays = 30
  generateForm.type = 'TRIAL'
  showGenerateModal.value = true
}

async function handleGenerate() {
  if (!generateForm.projectId) {
    ElMessage.warning('请选择项目')
    return
  }
  if (!generateForm.customerName.trim()) {
    ElMessage.warning('请输入客户名称')
    return
  }
  if (generateForm.type === 'TRIAL' && (!generateForm.trialDays || generateForm.trialDays < 1)) {
    ElMessage.warning('试用天数至少为1天')
    return
  }
  generateLoading.value = true
  try {
    const data = {
      projectId: generateForm.projectId,
      customerName: generateForm.customerName,
      type: generateForm.type
    }
    if (generateForm.type === 'TRIAL') {
      data.trialDays = generateForm.trialDays
    }
    await generateLicense(data)
    ElMessage.success('License生成成功')
    showGenerateModal.value = false
    loadLicenses()
  } catch (e) {
    console.error(e)
  } finally {
    generateLoading.value = false
  }
}

function handleDownload(item) {
  const url = getDownloadUrl(item.id)
  window.open(url, '_blank')
}

function openRenewModal(item) {
  renewTarget.value = item
  renewDays.value = item.trialDays || 30
  showRenewModal.value = true
}

async function handleRenew() {
  if (!renewDays.value || renewDays.value < 1) {
    ElMessage.warning('试用天数至少为1天')
    return
  }
  renewLoading.value = true
  try {
    await renewLicense(renewTarget.value.id, { trialDays: renewDays.value })
    ElMessage.success('续期成功')
    showRenewModal.value = false
    loadLicenses()
  } catch (e) {
    console.error(e)
  } finally {
    renewLoading.value = false
  }
}

onMounted(() => {
  loadProjects()
  loadLicenses()
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
