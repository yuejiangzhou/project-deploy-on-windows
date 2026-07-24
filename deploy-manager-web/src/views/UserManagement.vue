<template>
  <div class="p-6">
    <!-- Page Title Row -->
    <div class="flex items-center justify-between mb-6">
      <h1 class="font-heading text-xl" style="color: var(--color-text-primary);">用户管理</h1>
      <button class="flex items-center gap-1.5 px-4 py-2 rounded-md text-sm font-medium cursor-pointer border-0"
              style="background: var(--color-primary); color: var(--color-text-inverse);"
              @click="openCreateModal"
              @mouseenter="createBtnHover = true" @mouseleave="createBtnHover = false"
              :style="createBtnHover ? { background: 'var(--color-primary-hover)' } : {}">
        <Plus style="width: 16px; height: 16px;" />
        新增用户
      </button>
    </div>

    <!-- Search Bar -->
    <div class="flex items-center gap-3 mb-4">
      <div class="relative flex-1" style="max-width: 280px;">
        <Search style="width: 16px; height: 16px; color: var(--color-text-tertiary); position: absolute; left: 12px; top: 50%; transform: translateY(-50%);" />
        <input type="text" v-model="searchKeyword" placeholder="搜索用户名..."
               class="w-full pl-9 pr-3 py-2 rounded-md text-sm outline-none"
               style="background: var(--color-bg-elevated); border: 1px solid var(--color-border); color: var(--color-text-primary);"
               @focus="$event.target.style.borderColor='var(--color-primary)'"
               @blur="$event.target.style.borderColor='var(--color-border)'"
               @input="handleSearch">
      </div>
      <div class="flex items-center gap-2">
        <label class="text-sm" style="color: var(--color-text-secondary);">角色:</label>
        <select v-model="searchRole"
                class="px-3 py-2 rounded-md text-sm outline-none cursor-pointer"
                style="background: var(--color-bg-elevated); border: 1px solid var(--color-border); color: var(--color-text-primary); min-width: 120px;"
                @change="handleSearch">
          <option value="">全部</option>
          <option value="ADMIN">ADMIN</option>
          <option value="DEVELOPER">DEVELOPER</option>
          <option value="SALES">SALES</option>
        </select>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center py-8 text-sm" style="color: var(--color-text-tertiary);">加载中...</div>

    <!-- User Table -->
    <div v-if="!loading" class="rounded-lg overflow-hidden" style="background: var(--color-bg-elevated); border: 1px solid var(--color-border-light); box-shadow: var(--shadow-sm);">
      <table class="w-full" style="border-collapse: collapse;">
        <thead>
          <tr style="border-bottom: 1px solid var(--color-border-light);">
            <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">用户名</th>
            <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">显示名</th>
            <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">角色</th>
            <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">状态</th>
            <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">最后登录</th>
            <th class="text-left px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">创建时间</th>
            <th class="text-right px-4 py-3 text-xs font-medium" style="color: var(--color-text-tertiary); background: var(--color-bg-sunken);">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in userList" :key="u.id" class="table-row-hover" style="border-bottom: 1px solid var(--color-border-light);">
            <td class="px-4 py-3">
              <span class="text-sm font-medium" style="color: var(--color-text-primary);">{{ u.username }}</span>
            </td>
            <td class="px-4 py-3">
              <span class="text-sm" style="color: var(--color-text-secondary);">{{ u.displayName || '-' }}</span>
            </td>
            <td class="px-4 py-3">
              <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
                    :style="getRoleStyle(u.role)">
                {{ u.role }}
              </span>
            </td>
            <td class="px-4 py-3">
              <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
                    :style="u.status === 'ACTIVE'
                      ? { background: 'var(--state-success-bg)', color: 'var(--state-success)' }
                      : { background: 'var(--color-bg-sunken)', color: 'var(--color-text-tertiary)' }">
                {{ u.status === 'ACTIVE' ? '启用' : '禁用' }}
              </span>
            </td>
            <td class="px-4 py-3">
              <span class="text-sm" style="color: var(--color-text-secondary);">{{ formatDate(u.lastLogin) }}</span>
            </td>
            <td class="px-4 py-3">
              <span class="text-sm" style="color: var(--color-text-secondary);">{{ formatDate(u.createdAt) }}</span>
            </td>
            <td class="px-4 py-3 text-right">
              <div class="flex items-center justify-end gap-2">
                <button class="px-3 py-1 rounded text-xs font-medium cursor-pointer border-0"
                        style="background: var(--color-primary); color: var(--color-text-inverse);"
                        @click="openEditModal(u)">
                  编辑
                </button>
                <button class="px-3 py-1 rounded text-xs font-medium cursor-pointer"
                        style="border: 1px solid var(--color-border); color: var(--color-text-secondary); background: var(--color-bg-elevated);"
                        @click="openResetPwdModal(u)">
                  重置密码
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="userList.length === 0">
            <td colspan="7" class="px-4 py-8 text-center text-sm" style="color: var(--color-text-tertiary);">暂无用户</td>
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

    <!-- Create User Modal -->
    <div v-show="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
      <div class="modal-card" style="width: 420px;">
        <h2 class="font-heading text-lg" style="color: var(--color-text-primary); margin-bottom: 24px;">新增用户</h2>
        <div style="margin-bottom: 16px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">用户名 <span style="color: var(--color-danger);">*</span></label>
          <input type="text" v-model="createForm.username" placeholder="请输入用户名"
                 class="w-full box-border rounded-md text-sm outline-none"
                 style="padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-primary);"
                 @focus="$event.target.style.borderColor='var(--color-primary)'"
                 @blur="$event.target.style.borderColor='var(--color-border)'">
        </div>
        <div style="margin-bottom: 16px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">密码 <span style="color: var(--color-danger);">*</span></label>
          <input type="password" v-model="createForm.password" placeholder="请输入密码"
                 class="w-full box-border rounded-md text-sm outline-none"
                 style="padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-primary);"
                 @focus="$event.target.style.borderColor='var(--color-primary)'"
                 @blur="$event.target.style.borderColor='var(--color-border)'">
        </div>
        <div style="margin-bottom: 16px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">显示名</label>
          <input type="text" v-model="createForm.displayName" placeholder="请输入显示名"
                 class="w-full box-border rounded-md text-sm outline-none"
                 style="padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-primary);"
                 @focus="$event.target.style.borderColor='var(--color-primary)'"
                 @blur="$event.target.style.borderColor='var(--color-border)'">
        </div>
        <div style="margin-bottom: 24px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">角色 <span style="color: var(--color-danger);">*</span></label>
          <select v-model="createForm.role"
                  class="w-full box-border rounded-md text-sm outline-none cursor-pointer"
                  style="padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-primary); background: var(--color-bg-elevated);">
            <option value="">请选择角色</option>
            <option value="ADMIN">ADMIN</option>
            <option value="DEVELOPER">DEVELOPER</option>
            <option value="SALES">SALES</option>
          </select>
        </div>
        <div style="display: flex; justify-content: flex-end; gap: 8px;">
          <button class="rounded-md text-sm cursor-pointer"
                  style="padding: 6px 16px; border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-secondary);"
                  @click="showCreateModal = false">
            取消
          </button>
          <button class="rounded-md text-sm cursor-pointer border-0"
                  style="padding: 6px 16px; background: var(--color-primary); color: var(--color-text-inverse);"
                  :disabled="createLoading"
                  @click="handleCreate">
            {{ createLoading ? '创建中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Edit User Modal -->
    <div v-show="showEditModal" class="modal-overlay" @click.self="showEditModal = false">
      <div class="modal-card" style="width: 420px;">
        <h2 class="font-heading text-lg" style="color: var(--color-text-primary); margin-bottom: 24px;">编辑用户</h2>
        <p class="text-sm mb-4" style="color: var(--color-text-secondary);">用户名: {{ editTarget?.username }}</p>
        <div style="margin-bottom: 16px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">显示名</label>
          <input type="text" v-model="editForm.displayName" placeholder="请输入显示名"
                 class="w-full box-border rounded-md text-sm outline-none"
                 style="padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-primary);"
                 @focus="$event.target.style.borderColor='var(--color-primary)'"
                 @blur="$event.target.style.borderColor='var(--color-border)'">
        </div>
        <div style="margin-bottom: 16px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">角色</label>
          <select v-model="editForm.role"
                  class="w-full box-border rounded-md text-sm outline-none cursor-pointer"
                  style="padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-primary); background: var(--color-bg-elevated);">
            <option value="ADMIN">ADMIN</option>
            <option value="DEVELOPER">DEVELOPER</option>
            <option value="SALES">SALES</option>
          </select>
        </div>
        <div style="margin-bottom: 24px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">状态</label>
          <select v-model="editForm.status"
                  class="w-full box-border rounded-md text-sm outline-none cursor-pointer"
                  style="padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-primary); background: var(--color-bg-elevated);">
            <option value="ACTIVE">启用</option>
            <option value="DISABLED">禁用</option>
          </select>
        </div>
        <div style="display: flex; justify-content: flex-end; gap: 8px;">
          <button class="rounded-md text-sm cursor-pointer"
                  style="padding: 6px 16px; border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-secondary);"
                  @click="showEditModal = false">
            取消
          </button>
          <button class="rounded-md text-sm cursor-pointer border-0"
                  style="padding: 6px 16px; background: var(--color-primary); color: var(--color-text-inverse);"
                  :disabled="editLoading"
                  @click="handleEdit">
            {{ editLoading ? '保存中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Reset Password Modal -->
    <div v-show="showResetPwdModal" class="modal-overlay" @click.self="showResetPwdModal = false">
      <div class="modal-card" style="width: 420px;">
        <h2 class="font-heading text-lg" style="color: var(--color-text-primary); margin-bottom: 24px;">重置密码</h2>
        <p class="text-sm mb-4" style="color: var(--color-text-secondary);">用户: {{ pwdTarget?.username }}</p>
        <div style="margin-bottom: 16px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">旧密码</label>
          <input type="password" v-model="pwdForm.oldPassword" placeholder="请输入旧密码"
                 class="w-full box-border rounded-md text-sm outline-none"
                 style="padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-primary);"
                 @focus="$event.target.style.borderColor='var(--color-primary)'"
                 @blur="$event.target.style.borderColor='var(--color-border)'">
        </div>
        <div style="margin-bottom: 16px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">新密码 <span style="color: var(--color-danger);">*</span></label>
          <input type="password" v-model="pwdForm.newPassword" placeholder="请输入新密码"
                 class="w-full box-border rounded-md text-sm outline-none"
                 style="padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-primary);"
                 @focus="$event.target.style.borderColor='var(--color-primary)'"
                 @blur="$event.target.style.borderColor='var(--color-border)'">
        </div>
        <div style="margin-bottom: 24px;">
          <label class="block text-sm font-medium" style="color: var(--color-text-secondary); margin-bottom: 4px;">确认新密码 <span style="color: var(--color-danger);">*</span></label>
          <input type="password" v-model="pwdForm.confirmPassword" placeholder="请再次输入新密码"
                 class="w-full box-border rounded-md text-sm outline-none"
                 style="padding: 8px 12px; border: 1px solid var(--color-border); color: var(--color-text-primary);"
                 @focus="$event.target.style.borderColor='var(--color-primary)'"
                 @blur="$event.target.style.borderColor='var(--color-border)'">
        </div>
        <div style="display: flex; justify-content: flex-end; gap: 8px;">
          <button class="rounded-md text-sm cursor-pointer"
                  style="padding: 6px 16px; border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-secondary);"
                  @click="showResetPwdModal = false">
            取消
          </button>
          <button class="rounded-md text-sm cursor-pointer border-0"
                  style="padding: 6px 16px; background: var(--color-primary); color: var(--color-text-inverse);"
                  :disabled="pwdLoading"
                  @click="handleResetPwd">
            {{ pwdLoading ? '重置中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from 'lucide-vue-next'
import { getUserList, createUser, updateUser, resetPassword } from '@/api/user'
import { formatDate as fmtDate } from '@/utils/format'

const createBtnHover = ref(false)
const loading = ref(false)
const createLoading = ref(false)
const editLoading = ref(false)
const pwdLoading = ref(false)
const showCreateModal = ref(false)
const showEditModal = ref(false)
const showResetPwdModal = ref(false)

const searchKeyword = ref('')
const searchRole = ref('')
const page = ref(1)
const pageSize = 10
const total = ref(0)
const userList = ref([])
const editTarget = ref(null)
const pwdTarget = ref(null)

const searchTimeout = ref(null)

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

const createForm = reactive({
  username: '',
  password: '',
  displayName: '',
  role: ''
})

const editForm = reactive({
  displayName: '',
  role: '',
  status: 'ACTIVE'
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

function formatDate(dateStr) {
  return fmtDate(dateStr)
}

function getRoleStyle(role) {
  const map = {
    ADMIN: { background: 'var(--color-primary-light)', color: 'var(--color-primary)' },
    DEVELOPER: { background: 'var(--state-info-bg)', color: 'var(--state-info)' },
    SALES: { background: 'var(--state-success-bg)', color: 'var(--state-success)' }
  }
  return map[role] || { background: 'var(--color-bg-sunken)', color: 'var(--color-text-tertiary)' }
}

async function loadUsers() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (searchRole.value) params.role = searchRole.value
    const res = await getUserList(params)
    userList.value = res?.records || res || []
    total.value = res?.total || userList.value.length
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  if (searchTimeout.value) clearTimeout(searchTimeout.value)
  searchTimeout.value = setTimeout(() => {
    page.value = 1
    loadUsers()
  }, 300)
}

function changePage(p) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  loadUsers()
}

function openCreateModal() {
  createForm.username = ''
  createForm.password = ''
  createForm.displayName = ''
  createForm.role = ''
  showCreateModal.value = true
}

async function handleCreate() {
  if (!createForm.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!createForm.password) {
    ElMessage.warning('请输入密码')
    return
  }
  if (!createForm.role) {
    ElMessage.warning('请选择角色')
    return
  }
  createLoading.value = true
  try {
    await createUser({
      username: createForm.username,
      password: createForm.password,
      displayName: createForm.displayName,
      role: createForm.role
    })
    ElMessage.success('创建成功')
    showCreateModal.value = false
    loadUsers()
  } catch (e) {
    console.error(e)
  } finally {
    createLoading.value = false
  }
}

function openEditModal(u) {
  editTarget.value = u
  editForm.displayName = u.displayName || ''
  editForm.role = u.role || ''
  editForm.status = u.status || 'ACTIVE'
  showEditModal.value = true
}

async function handleEdit() {
  if (!editTarget.value) return
  editLoading.value = true
  try {
    await updateUser(editTarget.value.id, {
      displayName: editForm.displayName,
      role: editForm.role,
      status: editForm.status
    })
    ElMessage.success('保存成功')
    showEditModal.value = false
    loadUsers()
  } catch (e) {
    console.error(e)
  } finally {
    editLoading.value = false
  }
}

function openResetPwdModal(u) {
  pwdTarget.value = u
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  showResetPwdModal.value = true
}

async function handleResetPwd() {
  if (!pwdForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  pwdLoading.value = true
  try {
    await resetPassword(pwdTarget.value.id, {
      oldPassword: pwdForm.oldPassword || undefined,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码重置成功')
    showResetPwdModal.value = false
  } catch (e) {
    console.error(e)
  } finally {
    pwdLoading.value = false
  }
}

onMounted(() => {
  loadUsers()
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
