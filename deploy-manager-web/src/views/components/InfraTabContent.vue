<template>
  <div class="p-5">
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-2">
        <span class="text-sm font-medium" style="color: var(--color-text-primary);">{{ tabLabel }} 版本列表</span>
        <span class="text-xs px-2 py-0.5 rounded-full" style="background: var(--color-primary-light); color: var(--color-primary); font-weight: 500;">{{ components.length }} 个版本</span>
      </div>
      <button class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md transition-colors cursor-pointer btn-primary" @click="showUploadZone = !showUploadZone">
        <Plus style="width: 14px; height: 14px;" />
        上传新版本
      </button>
    </div>

    <div v-if="showUploadZone" class="mb-4">
      <div
        class="rounded-lg flex flex-col items-center justify-center cursor-pointer transition-colors upload-zone"
        @click="triggerUpload"
        @dragover.prevent="dragOver = true"
        @dragleave="dragOver = false"
        @drop.prevent="handleDrop"
        :class="{ 'drag-over': dragOver }"
      >
        <div class="mb-3 flex items-center justify-center rounded-full" style="width: 48px; height: 48px; background: var(--color-primary-light);">
          <UploadCloud style="width: 24px; height: 24px; color: var(--color-primary);" />
        </div>
        <p class="text-sm mb-1" style="color: var(--color-text-primary); font-weight: 500;">
          拖拽{{ tabLabel }}压缩包到此处
        </p>
        <p class="text-xs mb-3" style="color: var(--color-text-tertiary);">
          支持 zip / tar / tar.gz / 7z 格式，上传后自动解压
        </p>
        <div class="flex items-center gap-3 mb-3 flex-wrap justify-center">
          <div class="flex items-center gap-1.5">
            <label class="text-xs whitespace-nowrap" style="color: var(--color-text-secondary);">版本:</label>
            <input
              v-model="versionInput"
              type="text"
              placeholder="如 8.0.35（必填）"
              class="version-input"
              style="width: 140px;"
            />
          </div>
          <div class="flex items-center gap-1.5">
            <label class="text-xs whitespace-nowrap" style="color: var(--color-text-secondary);">标签:</label>
            <input
              v-model="tagInput"
              type="text"
              placeholder="如：推荐版本"
              class="version-input"
            />
          </div>
        </div>
        <button class="px-4 py-2 text-sm rounded-md transition-colors cursor-pointer btn-primary" @click.stop="triggerUpload">
          选择压缩包
        </button>
        <p class="text-xs mt-2" style="color: var(--color-text-tertiary);">{{ uploadTip }}</p>
      </div>
    </div>

    <div class="rounded-lg overflow-hidden" style="background: var(--color-bg-elevated); border: 1px solid var(--color-border-light); box-shadow: var(--shadow-sm);">
      <table class="w-full text-sm" style="border-collapse: collapse;">
        <thead>
          <tr style="background: var(--color-bg-sunken);">
            <th class="text-left px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">版本</th>
            <th class="text-left px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">标签</th>
            <th class="text-left px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">文件名</th>
            <th class="text-left px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">大小</th>
            <th class="text-left px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">上传时间</th>
            <th class="text-right px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in components" :key="item.id" class="table-row-hover" style="border-bottom: 1px solid var(--color-border-light);">
            <td class="px-5 py-3">
              <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium" style="background: var(--state-info-bg); color: var(--state-info);">
                {{ item.version || item.fileName }}
              </span>
            </td>
            <td class="px-5 py-3">
              <span class="text-xs px-2 py-0.5 rounded" style="background: var(--color-bg-sunken); color: var(--color-text-secondary);">
                {{ item.tag || '-' }}
              </span>
            </td>
            <td class="px-5 py-3">
              <div class="flex items-center gap-2">
                <component :is="getFileIcon()" style="width: 16px; height: 16px;" :style="{ color: getFileIconColor() }" />
                <span style="color: var(--color-text-primary);">{{ item.fileName }}</span>
              </div>
            </td>
            <td class="px-5 py-3" style="color: var(--color-text-secondary);">{{ formatSize(item.fileSize) }}</td>
            <td class="px-5 py-3" style="color: var(--color-text-secondary);">{{ formatDate(item.createdAt) }}</td>
            <td class="px-5 py-3 text-right">
              <button class="text-xs transition-colors cursor-pointer btn-delete-text" @click="handleDelete(item)">删除</button>
            </td>
          </tr>
          <tr v-if="!components.length">
            <td colspan="6" class="px-5 py-8 text-center" style="color: var(--color-text-tertiary);">
              暂无{{ tabLabel }}版本
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <input
      ref="fileInputRef"
      type="file"
      style="display: none;"
      accept=".zip,.tar,.tar.gz,.tgz,.7z"
      @change="handleFileChange"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, UploadCloud,
  Package, HardDrive, Database, Server, Cog
} from 'lucide-vue-next'
import { getInfraList, uploadInfraZip, deleteInfraFile } from '@/api/infrastructure'
import { formatFileSize } from '@/utils/format'

const props = defineProps({
  type: { type: String, required: true }
})

const components = ref([])
const dragOver = ref(false)
const fileInputRef = ref(null)
const showUploadZone = ref(false)
const versionInput = ref('')
const tagInput = ref('')

const tabLabel = computed(() => {
  const map = {
    jdk: 'JDK',
    mysql: 'MySQL',
    minio: 'MinIO',
    nginx: 'Nginx',
    engine: '引擎'
  }
  return map[props.type] || props.type
})

const uploadTip = computed(() => {
  const map = {
    jdk: 'JDK 纯净版压缩包，打包时自动解压',
    mysql: 'MySQL 便携版压缩包，请上传已初始化好的版本',
    minio: 'MinIO 压缩包',
    nginx: 'Nginx 压缩包',
    engine: '引擎压缩包'
  }
  return map[props.type] || ''
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

function getFileIcon() {
  if (props.type === 'jdk') return HardDrive
  if (props.type === 'mysql') return Database
  if (props.type === 'minio') return Server
  if (props.type === 'nginx') return Server
  if (props.type === 'engine') return Cog
  return Package
}

function getFileIconColor() {
  if (props.type === 'jdk') return 'var(--state-success)'
  if (props.type === 'mysql') return 'var(--state-warning)'
  if (props.type === 'minio') return 'var(--state-info)'
  if (props.type === 'nginx') return 'var(--state-success)'
  if (props.type === 'engine') return 'var(--color-primary)'
  return 'var(--state-info)'
}

async function loadComponents() {
  try {
    const res = await getInfraList(props.type)
    components.value = res || []
  } catch (e) {
    console.error(e)
  }
}

function triggerUpload() {
  fileInputRef.value?.click()
}

async function handleFileChange(e) {
  const files = e.target.files
  if (!files || !files.length) return
  await doUpload(files[0])
  e.target.value = ''
}

async function handleDrop(e) {
  dragOver.value = false
  const files = e.dataTransfer.files
  if (!files || !files.length) return
  await doUpload(files[0])
}

async function doUpload(file) {
  if (!versionInput.value.trim()) {
    ElMessage.warning('请填写版本号')
    return
  }
  const fileName = file.name.toLowerCase()
  if (!fileName.match(/\.(zip|tar|tar\.gz|tgz|7z)$/)) {
    ElMessage.error('只支持 zip/tar/tar.gz/7z 格式')
    return
  }
  try {
    await uploadInfraZip(props.type, file, {
      version: versionInput.value,
      tag: tagInput.value
    })
    ElMessage.success('上传成功')
    showUploadZone.value = false
    versionInput.value = ''
    tagInput.value = ''
    loadComponents()
  } catch (e) {
    console.error(e)
  }
}

async function handleDelete(item) {
  try {
    await ElMessageBox.confirm(`确定要删除「${item.fileName}」吗？`, '提示', { type: 'warning' })
    await deleteInfraFile(props.type, item.id)
    ElMessage.success('删除成功')
    loadComponents()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

watch(() => props.type, () => {
  showUploadZone.value = false
  loadComponents()
})

onMounted(() => {
  loadComponents()
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

.btn-delete-text {
  color: var(--state-error);
  background: none;
  border: none;
  padding: 0;
}
.btn-delete-text:hover {
  text-decoration: underline;
}

.upload-zone {
  border: 2px dashed var(--color-primary);
  background: var(--color-primary-light);
  padding: 32px 24px;
  min-height: 140px;
  border-radius: var(--radius-lg);
}
.upload-zone.drag-over {
  border-color: var(--color-primary-hover);
  background: #DBEAFE;
}

.version-input {
  border: 1px solid var(--color-border);
  background: var(--color-bg-elevated);
  color: var(--color-text-primary);
  outline: none;
  width: 100px;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 12px;
  transition: border-color 150ms;
}
.version-input:focus {
  border-color: var(--color-primary);
}

.table-row-hover:hover {
  background: var(--color-bg-sunken);
}
.table-row-hover {
  transition: background 150ms ease;
}
</style>
