<template>
  <main class="flex min-h-screen" style="background: var(--color-bg);">
    <AppSidebar />
    <div class="flex-1 flex flex-col overflow-hidden">
      <AppHeader :breadcrumbs="[{ text: '项目管理', link: '/projects' }, { text: projectName }, { text: '打包中心' }]" />
      <div class="flex-1 overflow-y-auto p-6" style="background: var(--color-bg);">

        <!-- SECTION 1: 当前配置概览 -->
        <section class="mb-6">
          <div class="rounded-xl p-6" style="background: var(--color-bg-elevated); box-shadow: var(--shadow-md);">
            <div class="flex items-start justify-between mb-2">
              <div>
                <h2 class="font-heading text-lg flex items-center gap-2" style="color: var(--color-text-primary);">
                  <Eye style="width: 20px; height: 20px; color: var(--color-primary);" />
                  当前配置
                </h2>
                <p class="text-sm mt-1" style="color: var(--color-text-tertiary);">以下为项目配置中已设定的组件版本</p>
              </div>
              <button class="inline-flex items-center gap-1.5 text-sm font-medium transition-colors hover:opacity-80 cursor-pointer" style="color: var(--color-primary);" @click="goToConfig">
                <ArrowLeft style="width: 14px; height: 14px;" />
                返回项目配置页
              </button>
            </div>

            <div class="rounded-lg border overflow-hidden mt-4" style="border-color: var(--color-border-light); background: var(--color-bg);">
              <div v-if="projectConfig.jarFileName" class="config-row border-b" style="border-color: var(--color-border-light);">
                <Box style="width: 16px; height: 16px; color: var(--color-primary); flex-shrink: 0;" />
                <span class="text-sm font-medium flex-shrink-0" style="color: var(--color-text-primary); min-width: 120px;">后端JAR包</span>
                <span class="text-sm" style="color: var(--color-text-primary);">{{ projectConfig.jarFileName }}</span>
                <span v-if="projectConfig.jarFileSize" class="text-xs px-2 py-0.5 rounded-md" style="background: var(--color-bg-sunken); color: var(--color-text-secondary);">{{ formatSize(projectConfig.jarFileSize) }}</span>
                <span class="text-xs" style="color: var(--color-text-tertiary);">上传: {{ projectConfig.jarUploadDate || '' }}</span>
                <span class="text-xs ml-auto flex-shrink-0" style="color: var(--color-text-tertiary);">端口:{{ projectConfig.appPort || 8080 }}</span>
              </div>

              <div v-if="projectConfig.vueFileName" class="config-row border-b" style="border-color: var(--color-border-light);">
                <Layout style="width: 16px; height: 16px; color: var(--color-primary); flex-shrink: 0;" />
                <span class="text-sm font-medium flex-shrink-0" style="color: var(--color-text-primary); min-width: 120px;">Vue前端</span>
                <span class="text-sm" style="color: var(--color-text-primary);">{{ projectConfig.vueFileName }}</span>
                <span v-if="projectConfig.vueFileSize" class="text-xs px-2 py-0.5 rounded-md" style="background: var(--color-bg-sunken); color: var(--color-text-secondary);">{{ formatSize(projectConfig.vueFileSize) }}</span>
                <span class="text-xs" style="color: var(--color-text-tertiary);">上传: {{ projectConfig.vueUploadDate || '' }}</span>
              </div>

              <div v-if="projectConfig.includeJdk && projectConfig.jdkFileName" class="config-row border-b" style="border-color: var(--color-border-light);">
                <Cpu style="width: 16px; height: 16px; color: var(--color-primary); flex-shrink: 0;" />
                <span class="text-sm font-medium flex-shrink-0" style="color: var(--color-text-primary); min-width: 120px;">JDK</span>
                <span class="text-sm" style="color: var(--color-text-primary);">{{ projectConfig.jdkFileName }}</span>
                <span v-if="projectConfig.jdkVersion" class="text-xs px-2 py-0.5 rounded-md" style="background: var(--state-info-bg); color: var(--state-info);">{{ projectConfig.jdkVersion }}</span>
                <span v-if="projectConfig.jdkFileSize" class="text-xs px-2 py-0.5 rounded-md" style="background: var(--color-bg-sunken); color: var(--color-text-secondary);">{{ formatSize(projectConfig.jdkFileSize) }}</span>
              </div>

              <div v-if="projectConfig.includeMysql && projectConfig.mysqlFileName" class="config-row border-b" style="border-color: var(--color-border-light);">
                <Database style="width: 16px; height: 16px; color: var(--color-primary); flex-shrink: 0;" />
                <span class="text-sm font-medium flex-shrink-0" style="color: var(--color-text-primary); min-width: 120px;">MySQL</span>
                <span class="text-sm" style="color: var(--color-text-primary);">{{ projectConfig.mysqlFileName }}</span>
                <span v-if="projectConfig.mysqlVersion" class="text-xs px-2 py-0.5 rounded-md" style="background: var(--state-info-bg); color: var(--state-info);">{{ projectConfig.mysqlVersion }}</span>
                <span class="text-xs" style="color: var(--color-text-tertiary);">端口:{{ projectConfig.mysqlPort || 3306 }}</span>
                <span class="flex items-center gap-1 text-xs px-2 py-0.5 rounded ml-auto flex-shrink-0" style="background: var(--state-info-bg); color: var(--state-info);">
                  <Lock style="width: 10px; height: 10px;" /> 账密加密
                </span>
              </div>

              <div v-if="projectConfig.includeMinio && projectConfig.minioFileName" class="config-row border-b" style="border-color: var(--color-border-light);">
                <HardDrive style="width: 16px; height: 16px; color: var(--color-primary); flex-shrink: 0;" />
                <span class="text-sm font-medium flex-shrink-0" style="color: var(--color-text-primary); min-width: 120px;">MinIO</span>
                <span class="text-sm" style="color: var(--color-text-primary);">{{ projectConfig.minioFileName }}</span>
                <span v-if="projectConfig.minioVersion" class="text-xs px-2 py-0.5 rounded-md" style="background: var(--state-info-bg); color: var(--state-info);">{{ projectConfig.minioVersion }}</span>
                <span class="text-xs" style="color: var(--color-text-tertiary);">API:{{ projectConfig.minioApiPort || 9000 }} Console:{{ projectConfig.minioConsolePort || 9001 }}</span>
                <span class="flex items-center gap-1 text-xs px-2 py-0.5 rounded ml-auto flex-shrink-0" style="background: var(--state-info-bg); color: var(--state-info);">
                  <Lock style="width: 10px; height: 10px;" /> 账密加密
                </span>
              </div>

              <div v-if="projectConfig.includeNginx && projectConfig.nginxFileName" class="config-row border-b" style="border-color: var(--color-border-light);">
                <Globe style="width: 16px; height: 16px; color: var(--color-primary); flex-shrink: 0;" />
                <span class="text-sm font-medium flex-shrink-0" style="color: var(--color-text-primary); min-width: 120px;">Nginx</span>
                <span class="text-sm" style="color: var(--color-text-primary);">{{ projectConfig.nginxFileName }}</span>
                <span v-if="projectConfig.nginxVersion" class="text-xs px-2 py-0.5 rounded-md" style="background: var(--state-info-bg); color: var(--state-info);">{{ projectConfig.nginxVersion }}</span>
                <span class="text-xs ml-auto flex-shrink-0" style="color: var(--color-text-tertiary);">HTTP端口:{{ projectConfig.nginxHttpPort || 80 }}</span>
              </div>

              <div v-if="projectConfig.engineEnabled && projectConfig.engineFileName" class="config-row border-b" style="border-color: var(--color-border-light);">
                <Cog style="width: 16px; height: 16px; color: var(--color-primary); flex-shrink: 0;" />
                <span class="text-sm font-medium flex-shrink-0" style="color: var(--color-text-primary); min-width: 120px;">引擎</span>
                <span class="text-sm" style="color: var(--color-text-primary);">{{ projectConfig.engineFileName }}</span>
                <span v-if="projectConfig.engineVersion" class="text-xs px-2 py-0.5 rounded-md" style="background: var(--state-info-bg); color: var(--state-info);">{{ projectConfig.engineVersion }}</span>
                <span class="text-xs ml-auto flex-shrink-0" style="color: var(--color-text-tertiary);">端口:{{ projectConfig.enginePort || 8090 }}</span>
              </div>

              <!-- 未配置提示 -->
              <div v-if="!projectConfig.jarFileName && !projectConfig.vueFileName && !projectConfig.mysqlFileName" class="config-row">
                <Info style="width: 16px; height: 16px; color: var(--color-text-tertiary); flex-shrink: 0;" />
                <span class="text-sm" style="color: var(--color-text-tertiary);">暂无已保存的配置，请先前往项目配置页选择组件</span>
              </div>

              <div class="config-row">
                <Terminal style="width: 16px; height: 16px; color: var(--state-success); flex-shrink: 0;" />
                <span class="text-sm font-medium flex-shrink-0" style="color: var(--color-text-primary); min-width: 120px;">启动脚本系统</span>
                <span class="text-xs" style="color: var(--color-text-tertiary);">start.ps1 / stop.ps1 / PID管理</span>
              </div>
            </div>

            <!-- ZIP加密设置 -->
            <div class="rounded-xl border p-5 mt-5" style="border-color: var(--color-border-light); background: var(--color-bg);">
              <h3 class="text-sm font-medium mb-4 flex items-center gap-2" style="color: var(--color-text-primary);">
                <ShieldCheck style="width: 16px; height: 16px; color: var(--color-primary);" />
                ZIP加密设置
              </h3>
              <div class="grid grid-cols-2 gap-x-6 gap-y-4">
                <div>
                  <label class="block text-xs font-medium mb-1.5" style="color: var(--color-text-secondary);">密码</label>
                  <div class="relative">
                    <el-input
                      v-model="zipPassword"
                      type="password"
                      placeholder="请输入加密密码"
                      show-password
                    />
                  </div>
                </div>
                <div>
                  <label class="block text-xs font-medium mb-1.5" style="color: var(--color-text-secondary);">确认密码</label>
                  <div class="relative">
                    <el-input
                      v-model="zipPasswordConfirm"
                      type="password"
                      placeholder="请再次输入密码"
                      show-password
                    />
                  </div>
                </div>
              </div>
              <div class="mt-4 flex items-start gap-2 rounded-lg px-3 py-2.5 text-xs" style="background: var(--color-primary-light); color: var(--color-primary-text);">
                <Info style="width: 14px; height: 14px; flex-shrink: 0; margin-top: 1px;" />
                密码用于ZIP加密
              </div>
              <div class="mt-5">
                <button
                  class="inline-flex items-center gap-2 px-5 py-2.5 rounded-lg text-sm font-medium text-white transition-colors hover:opacity-90 cursor-pointer btn-primary"
                  :disabled="isPackaging"
                  @click="startPackage"
                >
                  <Package style="width: 16px; height: 16px;" />
                  {{ isPackaging ? '打包中...' : '开始打包' }}
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- SECTION 2: 打包进度 -->
        <section v-if="isPackaging || packageLogs.length" class="mb-6">
          <div class="rounded-xl p-6" style="background: var(--color-bg-elevated); box-shadow: var(--shadow-md);">
            <h2 class="font-heading text-lg mb-4 flex items-center gap-2" style="color: var(--color-text-primary);">
              <Loader style="width: 20px; height: 20px; color: var(--color-primary);" :class="{ 'animate-spin': isPackaging }" />
              打包进度
            </h2>
            <div class="mb-2">
              <div class="flex items-center justify-between text-sm mb-2">
                <span class="flex items-center gap-2" style="color: var(--color-text-secondary);">
                  <span class="inline-block w-2 h-2 rounded-full" :class="{ 'animate-pulse': isPackaging }" style="background: var(--color-primary);"></span>
                  {{ currentStep || '等待开始' }}
                </span>
                <span class="font-medium" style="color: var(--color-primary);">{{ progress }}%</span>
              </div>
              <div class="w-full h-2.5 rounded-full overflow-hidden" style="background: var(--color-bg-muted);">
                <div class="h-full rounded-full transition-all" :style="{ width: progress + '%', background: 'var(--color-primary)' }"></div>
              </div>
            </div>
            <div class="mt-4 rounded-lg p-4 overflow-auto text-xs leading-relaxed terminal-log" ref="terminalRef">
              <div v-if="!packageLogs.length" style="color: #64748B;">等待打包开始...</div>
              <div v-for="(log, idx) in packageLogs" :key="idx">
                <span style="color: #64748B;">[{{ log.time }}]</span>
                <span :style="{ color: getLogColor(log.type) }"> {{ log.text }}</span>
                <span v-if="log.ok" style="color: #16A34A;"> OK</span>
              </div>
            </div>
          </div>
        </section>

        <!-- SECTION 3: 打包产物列表 -->
        <section>
          <div class="rounded-xl p-6" style="background: var(--color-bg-elevated); box-shadow: var(--shadow-md);">
            <div class="flex items-center justify-between mb-5">
              <h2 class="font-heading text-lg flex items-center gap-2" style="color: var(--color-text-primary);">
                <Archive style="width: 20px; height: 20px; color: var(--color-primary);" />
                打包产物列表
              </h2>
              <div class="flex items-center gap-1 p-1 rounded-lg" style="background: var(--color-bg);">
                <button
                  class="filter-btn px-3 py-1.5 rounded-md text-xs font-medium cursor-pointer transition-colors"
                  :class="{ active: filterStatus === 'all' }"
                  @click="filterStatus = 'all'"
                >全部</button>
                <button
                  class="filter-btn px-3 py-1.5 rounded-md text-xs font-medium cursor-pointer transition-colors"
                  :class="{ active: filterStatus === 'SUCCESS' }"
                  @click="filterStatus = 'SUCCESS'"
                >成功</button>
                <button
                  class="filter-btn px-3 py-1.5 rounded-md text-xs font-medium cursor-pointer transition-colors"
                  :class="{ active: filterStatus === 'FAILED' }"
                  @click="filterStatus = 'FAILED'"
                >失败</button>
              </div>
            </div>

            <div class="rounded-lg border overflow-hidden" style="border-color: var(--color-border-light);">
              <table class="w-full text-sm">
                <thead>
                  <tr style="background: var(--color-bg-sunken);">
                    <th class="text-left px-4 py-2.5 text-xs font-medium" style="color: var(--color-text-tertiary);">文件名</th>
                    <th class="text-left px-4 py-2.5 text-xs font-medium" style="color: var(--color-text-tertiary);">大小</th>
                    <th class="text-left px-4 py-2.5 text-xs font-medium" style="color: var(--color-text-tertiary);">加密</th>
                    <th class="text-left px-4 py-2.5 text-xs font-medium" style="color: var(--color-text-tertiary);">时间</th>
                    <th class="text-left px-4 py-2.5 text-xs font-medium" style="color: var(--color-text-tertiary);">状态</th>
                    <th class="text-right px-4 py-2.5 text-xs font-medium" style="color: var(--color-text-tertiary);">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="pkg in filteredPackages" :key="pkg.id" class="border-t table-row-hover" style="border-color: var(--color-border-light);">
                    <td class="px-4 py-3">
                      <div class="flex items-center gap-2">
                        <FileArchive style="width: 16px; height: 16px; color: var(--color-primary);" />
                        <span class="font-medium" style="color: var(--color-text-primary);">{{ pkg.fileName }}</span>
                      </div>
                    </td>
                    <td class="px-4 py-3" style="color: var(--color-text-secondary);">{{ formatSize(pkg.fileSize) }}</td>
                    <td class="px-4 py-3">
                      <span class="inline-flex items-center gap-1" :style="{ color: pkg.encrypted ? 'var(--state-success)' : 'var(--color-text-tertiary)' }">
                        <Lock v-if="pkg.encrypted" style="width: 12px; height: 12px;" />
                        <Unlock v-else style="width: 12px; height: 12px;" />
                        {{ pkg.encrypted ? '是' : '否' }}
                      </span>
                    </td>
                    <td class="px-4 py-3" style="color: var(--color-text-secondary);">{{ formatDate(pkg.createdAt) }}</td>
                    <td class="px-4 py-3">
                      <span
                        class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
                        :style="pkg.status === 'SUCCESS'
                          ? { background: 'var(--state-success-bg)', color: 'var(--state-success)' }
                          : { background: 'var(--state-error-bg)', color: 'var(--state-error)' }"
                      >
                        {{ pkg.status === 'SUCCESS' ? '成功' : '失败' }}
                      </span>
                    </td>
                    <td class="px-4 py-3 text-right">
                      <button
                        v-if="pkg.status === 'SUCCESS'"
                        class="inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-medium transition-colors cursor-pointer btn-download"
                        @click="handleDownload(pkg)"
                      >
                        <Download style="width: 13px; height: 13px;" /> 下载
                      </button>
                      <button
                        class="inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-medium transition-colors cursor-pointer btn-delete"
                        @click="handleDeletePackage(pkg)"
                      >
                        <Trash2 style="width: 13px; height: 13px;" /> 删除
                      </button>
                    </td>
                  </tr>
                  <tr v-if="!filteredPackages.length">
                    <td colspan="6" class="px-4 py-8 text-center" style="color: var(--color-text-tertiary);">
                      暂无打包记录
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>

      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Eye, ArrowLeft, Box, Layout, Cpu, Database, HardDrive, Cog, Globe,
  Terminal, ShieldCheck, Info, Package, Loader, Archive, FileArchive,
  Lock, Unlock, Download, Trash2
} from 'lucide-vue-next'
import AppSidebar from '@/components/AppSidebar.vue'
import AppHeader from '@/components/AppHeader.vue'
import {
  getProjectConfig, getProject
} from '@/api/project'
import {
  getPackages, startPackage as startPackageApi, getDownloadUrl, deletePackage, getPackageProgress
} from '@/api/package'
import { formatFileSize } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => route.params.id)

const projectName = ref('')
const projectConfig = reactive({
  jarFileId: null,
  jarFileName: '',
  jarFileSize: 0,
  jarUploadDate: '',
  vueFolderId: null,
  vueFileName: '',
  vueFileSize: 0,
  vueUploadDate: '',
  appPort: 8080,
  jvmParams: '',
  mysqlPort: 3306,
  dbName: '',
  dbUsername: '',
  dbPassword: '',
  minioApiPort: 9000,
  minioConsolePort: 9001,
  minioAccessKey: '',
  minioSecretKey: '',
  nginxHttpPort: 80,
  nginxConfContent: '',
  engineEnabled: false,
  enginePort: 8090,
  includeJdk: false,
  includeMysql: false,
  includeMinio: false,
  includeNginx: false,
  // 组件ID
  jdkComponentId: null,
  mysqlComponentId: null,
  minioComponentId: null,
  nginxComponentId: null,
  engineComponentId: null,
  // 组件文件信息（由后端 fillFileInfo 填充）
  jdkFileName: '',
  jdkFileSize: 0,
  jdkVersion: '',
  mysqlFileName: '',
  mysqlFileSize: 0,
  mysqlVersion: '',
  minioFileName: '',
  minioFileSize: 0,
  minioVersion: '',
  nginxFileName: '',
  nginxFileSize: 0,
  nginxVersion: '',
  engineFileName: '',
  engineFileSize: 0,
  engineVersion: ''
})
const zipPassword = ref('')
const zipPasswordConfirm = ref('')
const isPackaging = ref(false)
const progress = ref(0)
const currentStep = ref('')
const packageLogs = ref([])
const packageRecords = ref([])
const filterStatus = ref('all')
const terminalRef = ref(null)

let progressTimer = null
let pollFailCount = 0

const filteredPackages = computed(() => {
  if (filterStatus.value === 'all') return packageRecords.value
  return packageRecords.value.filter(p => p.status === filterStatus.value)
})

function formatSize(size) {
  return formatFileSize(size)
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function getLogColor(type) {
  if (type === 'error') return '#DC2626'
  if (type === 'success') return '#16A34A'
  if (type === 'info') return '#FFFFFF'
  return '#E2E8F0'
}

function goToConfig() {
  router.push(`/projects/${projectId.value}/config`)
}

async function loadProject() {
  try {
    const project = await getProject(projectId.value)
    projectName.value = project?.name || '项目'
  } catch (e) {
    console.error(e)
  }
}

async function loadConfig() {
  try {
    const config = await getProjectConfig(projectId.value)
    Object.assign(projectConfig, config)
  } catch (e) {
    console.error(e)
  }
}

async function loadPackages() {
  try {
    const res = await getPackages({ projectId: projectId.value })
    packageRecords.value = res?.records || res || []
  } catch (e) {
    console.error(e)
  }
}

function addLog(text, type = 'normal', ok = false) {
  const now = new Date()
  const time = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
  packageLogs.value.push({ time, text, type, ok })
  nextTick(() => {
    if (terminalRef.value) {
      terminalRef.value.scrollTop = terminalRef.value.scrollHeight
    }
  })
}

async function startPackage() {
  if (!zipPassword.value.trim()) {
    ElMessage.warning('请输入加密密码')
    return
  }
  if (zipPassword.value !== zipPasswordConfirm.value) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  if (zipPassword.value.length < 6) {
    ElMessage.warning('加密密码长度不能少于6位')
    return
  }

  isPackaging.value = true
  progress.value = 0
  packageLogs.value = []
  currentStep.value = '正在启动打包任务...'
  addLog(`开始打包 ${projectName.value}...`, 'info')

  let taskId = null
  try {
    const res = await startPackageApi({ projectId: projectId.value, password: zipPassword.value })
    taskId = res?.taskId || res?.data?.taskId || res
    if (!taskId) {
      throw new Error('未获取到打包任务ID')
    }
    addLog('打包任务已创建，任务ID: ' + taskId, 'info')

    await pollPackageProgress(taskId)

    addLog('打包完成！', 'success')
    ElMessage.success('打包成功')
    loadPackages()
  } catch (e) {
    console.error(e)
    addLog('打包失败: ' + (e?.message || '未知错误'), 'error')
    ElMessage.error('打包失败: ' + (e?.message || ''))
  } finally {
    isPackaging.value = false
  }
}

async function pollPackageProgress(taskId) {
  return new Promise((resolve, reject) => {
    progressTimer = setInterval(async () => {
      try {
        const res = await getPackageProgress(taskId)
        const data = res?.data || res
        if (!data) return

        pollFailCount = 0
        progress.value = data.progress || 0
        currentStep.value = data.currentStep || currentStep.value

        const newLogs = data.logs || []
        if (newLogs.length > packageLogs.value.length) {
          const oldLast = packageLogs.value.length > 0 ? packageLogs.value[packageLogs.value.length - 1] : null
          const newLast = newLogs.length > 0 ? newLogs[newLogs.length - 1] : null
          if (!oldLast || !newLast || oldLast.time !== newLast.time) {
            const extraLogs = newLogs.slice(packageLogs.value.length)
            extraLogs.forEach(log => {
              packageLogs.value.push({
                time: log.time || '',
                text: log.message || '',
                type: log.level || 'normal',
                ok: false
              })
            })
            nextTick(() => {
              if (terminalRef.value) {
                terminalRef.value.scrollTop = terminalRef.value.scrollHeight
              }
            })
          }
        }

        if (data.status === 'SUCCESS') {
          clearInterval(progressTimer)
          progressTimer = null
          progress.value = 100
          resolve()
        } else if (data.status === 'FAILED') {
          clearInterval(progressTimer)
          progressTimer = null
          reject(new Error(data.currentStep || '打包失败'))
        }
      } catch (e) {
        pollFailCount++
        console.error('获取打包进度失败:', e)
        if (pollFailCount >= 3) {
          clearInterval(progressTimer)
          progressTimer = null
          ElMessage.error('获取打包进度连续失败，已停止自动刷新，请手动刷新页面')
        }
      }
    }, 1000)
  })
}

async function handleDownload(pkg) {
  const url = getDownloadUrl(pkg.id)
  window.open(url, '_blank')
}

async function handleDeletePackage(pkg) {
  try {
    await ElMessageBox.confirm(`确定要删除「${pkg.fileName}」吗？`, '提示', { type: 'warning' })
    await deletePackage(pkg.id)
    ElMessage.success('删除成功')
    loadPackages()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

onBeforeUnmount(() => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
})

onMounted(() => {
  loadProject()
  loadConfig()
  loadPackages()
})
</script>

<style scoped>
.btn-primary {
  background: var(--color-primary);
  color: #FFF;
  border: none;
  transition: background 150ms;
}
.btn-primary:hover {
  background: var(--color-primary-hover);
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.config-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  transition: background 0.15s;
}
.config-row:hover { background: var(--color-bg-sunken); }

.terminal-log {
  background: #0F172A;
  font-family: var(--font-mono, 'JetBrains Mono', monospace);
  max-height: 240px;
  min-height: 120px;
}

.filter-btn {
  background: transparent;
  color: var(--color-text-secondary);
  border: none;
}
.filter-btn:hover {
  background: var(--color-bg-muted);
}
.filter-btn.active {
  background: var(--color-primary) !important;
  color: #FFFFFF !important;
  font-weight: 500;
}

.btn-download {
  color: var(--color-primary);
  background: transparent;
  border: none;
  margin-right: 4px;
}
.btn-download:hover {
  background: #EFF6FF;
}

.btn-delete {
  color: var(--state-error);
  background: transparent;
  border: none;
}
.btn-delete:hover {
  background: #FEF2F2;
}

.table-row-hover:hover {
  background: var(--color-bg-sunken);
}
.table-row-hover {
  transition: background 150ms ease;
}

.animate-spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.animate-pulse {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.grid-cols-2 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
}
</style>
