<template>
  <div class="p-8" style="background: var(--color-bg);">

        <!-- Page Title -->
        <h1 class="font-heading text-2xl mb-1" style="color: var(--color-text-primary);">资源上传</h1>
        <p class="text-sm mb-8" style="color: var(--color-text-secondary);">管理项目文件和基础组件</p>

        <!-- ============================================ -->
        <!-- Category 1: 项目文件 (Folder Tree Layout)     -->
        <!-- ============================================ -->
        <div class="mb-8">
          <div class="rounded-lg px-6 py-4 mb-4" style="background: var(--color-bg-elevated); border: 1px solid var(--color-border-light); box-shadow: var(--shadow-sm);">
            <div class="flex items-center gap-3 mb-1">
              <div class="flex items-center justify-center rounded-md" style="width: 36px; height: 36px; background: var(--color-primary-light);">
                <FolderOpen style="width: 18px; height: 18px; color: var(--color-primary);" />
              </div>
              <div>
                <h2 class="font-heading text-base" style="color: var(--color-text-primary);">项目文件</h2>
                <p class="text-xs" style="color: var(--color-text-tertiary);">上传各项目的JAR包和Vue前端产物</p>
              </div>
            </div>
          </div>

          <!-- Two-panel layout: Folder Tree + File List -->
          <div class="flex gap-4" style="min-height: 520px;">

            <!-- Left Panel: Folder Tree (300px) -->
            <div class="shrink-0 overflow-hidden" style="width: 300px; background: var(--color-bg-elevated); border: 1px solid var(--color-border-light); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm);">
              <div class="px-4 py-3 text-xs font-medium flex items-center gap-2" style="background: var(--color-bg-sunken); border-bottom: 1px solid var(--color-border-light); color: var(--color-text-secondary);">
                <FolderTree style="width: 14px; height: 14px;" />
                目录结构
              </div>
              <div class="py-2 overflow-y-auto no-scrollbar" style="max-height: 470px;">

                <div class="px-2" v-for="p in projectTree" :key="'p-' + p.id" style="padding-left: 4px;">
                  <div class="tree-node" @click="toggleProject(p)" :class="{ selected: false }">
                    <ChevronRight style="width: 16px; height: 16px;" class="tree-chevron" :class="{ expanded: p.expanded }" />
                    <Folder style="width: 16px; height: 16px;" class="tree-folder-icon" />
                    <span class="tree-label">{{ p.name }}</span>
                  </div>
                  <div class="tree-children" :class="{ collapsed: !p.expanded }" style="padding-left: 12px;">
                    <div class="tree-node" :class="{ selected: isProjectFileSelected(p.id, 'jar') }"
                         @click.stop="selectProjectFile(p, 'jar')">
                      <ChevronRight style="width: 16px; height: 16px; visibility: hidden;" />
                      <Folder style="width: 16px; height: 16px;" class="tree-folder-icon" />
                      <span class="tree-label">JAR 包</span>
                      <span class="text-xs ml-auto mr-2" style="color: var(--color-text-tertiary);">{{ getJarCount(p.id) }} 个文件</span>
                    </div>
                    <div class="tree-node" :class="{ selected: isProjectFileSelected(p.id, 'vue') }"
                         @click.stop="selectProjectFile(p, 'vue')">
                      <ChevronRight style="width: 16px; height: 16px; visibility: hidden;" />
                      <Folder style="width: 16px; height: 16px;" class="tree-folder-icon" />
                      <span class="tree-label">Vue 产物</span>
                      <span class="text-xs ml-auto mr-2" style="color: var(--color-text-tertiary);">{{ getVueCount(p.id) }} 个文件</span>
                    </div>
                  </div>
                </div>

              </div>
            </div>

            <!-- Right Panel: File List -->
            <div class="flex-1 overflow-hidden flex flex-col" style="background: var(--color-bg-elevated); border: 1px solid var(--color-border-light); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm);">

              <!-- Breadcrumb header -->
              <div class="px-5 py-3 flex items-center gap-2 shrink-0" style="border-bottom: 1px solid var(--color-border-light);">
                <span class="text-xs font-medium" style="color: var(--color-text-secondary);">{{ selectedProjectName }}</span>
                <ChevronRight style="width: 12px; height: 12px; color: var(--color-text-tertiary);" />
                <span class="text-xs font-medium" style="color: var(--color-text-primary);">{{ selectedFileType === 'jar' ? 'JAR 包' : 'Vue 产物' }}</span>
              </div>

              <div class="flex-1 overflow-y-auto p-5">

                <!-- Upload Zone -->
                <div class="rounded-lg flex flex-col items-center justify-center cursor-pointer transition-colors mb-5"
                     style="border: 2px dashed var(--color-border); background: var(--color-bg-elevated); padding: 32px 24px; min-height: 140px;"
                     :style="(uploadHover || dragOver) ? { borderColor: 'var(--color-primary)', background: 'var(--color-primary-light)' } : {}"
                     @mouseenter="uploadHover = true"
                     @mouseleave="uploadHover = false"
                     @dragover.prevent="dragOver = true"
                     @dragleave.prevent="dragOver = false"
                     @drop.prevent="handleDrop">
                  <div class="mb-2 flex items-center justify-center rounded-full" style="width: 40px; height: 40px; background: var(--color-primary-light);">
                    <component :is="selectedFileType === 'jar' ? UploadCloud : FolderUp" style="width: 20px; height: 20px; color: var(--color-primary);" />
                  </div>
                  <p class="text-sm mb-1" style="color: var(--color-text-primary); font-weight: 500;">
                    {{ selectedFileType === 'jar' ? '拖拽JAR文件到此处' : '拖拽Vue dist文件夹' }}
                  </p>
                  <p class="text-xs mb-3" style="color: var(--color-text-tertiary);">
                    {{ selectedFileType === 'jar' ? '.jar' : '选择文件夹上传' }}
                  </p>
                  <div class="flex items-center gap-3 flex-wrap justify-center">
                    <input type="text" v-model="uploadVersionTag" placeholder="版本标签，如 v2.1.0" class="text-xs px-2.5 py-1.5 rounded-md outline-none"
                           style="border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-primary); width: 160px;"
                           @focus="$event.target.style.borderColor='var(--color-primary)'"
                           @blur="$event.target.style.borderColor='var(--color-border)'">
                    <button class="px-4 py-2 text-sm rounded-md transition-colors cursor-pointer border-0"
                            style="background: var(--color-primary); color: #FFF;"
                            @click="handleUploadClick"
                            @mouseenter="$event.target.style.background='var(--color-primary-hover)'"
                            @mouseleave="$event.target.style.background='var(--color-primary)'">
                      {{ selectedFileType === 'jar' ? '上传' : '选择文件夹' }}
                    </button>
                    <input type="file" ref="fileInputRef" style="display: none;"
                           :accept="selectedFileType === 'jar' ? '.jar' : ''"
                           :webkitdirectory="selectedFileType === 'vue'"
                           @change="handleFileSelected($event)">
                  </div>
                </div>

                <!-- Warning: same-name overwrite -->
                <div class="rounded-md px-3 py-2 mb-4 flex items-center gap-2" style="background: var(--state-warning-bg); border: 1px solid #FDE68A;">
                  <AlertTriangle style="width: 13px; height: 13px; color: var(--state-warning); flex-shrink: 0;" />
                  <span class="text-xs" style="color: var(--state-warning);">同版本标签的文件将覆盖已存在的同名文件，请谨慎上传</span>
                </div>

                <!-- File Table -->
                <div v-if="currentFiles.length > 0" class="rounded-lg overflow-hidden" style="border: 1px solid var(--color-border-light);">
                  <table class="w-full text-sm" style="border-collapse: collapse;">
                    <thead>
                      <tr style="background: var(--color-bg-sunken);">
                        <th class="text-left px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">文件名</th>
                        <th class="text-left px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">版本标签</th>
                        <th class="text-left px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">大小</th>
                        <th class="text-left px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">上传时间</th>
                        <th class="text-right px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="f in currentFiles" :key="f.id" class="table-row-hover" :style="{ borderBottom: '1px solid var(--color-border-light)' }">
                        <td class="px-5 py-3">
                          <div class="flex items-center gap-2">
                            <component :is="selectedFileType === 'jar' ? FileBox : Folder"
                                       style="width: 16px; height: 16px;"
                                       :style="{ color: selectedFileType === 'jar' ? 'var(--state-info)' : 'var(--state-warning)' }" />
                            <span style="color: var(--color-text-primary);">{{ f.fileName }}</span>
                          </div>
                        </td>
                        <td class="px-5 py-3">
                          <span class="text-xs px-2 py-0.5 rounded" style="background: var(--color-bg-sunken); color: var(--color-text-secondary);">
                            {{ f.versionTag || '-' }}
                          </span>
                        </td>
                        <td class="px-5 py-3" style="color: var(--color-text-secondary);">{{ formatSize(f.fileSize) }}</td>
                        <td class="px-5 py-3" style="color: var(--color-text-secondary);">{{ formatDate(f.createdAt) }}</td>
                        <td class="px-5 py-3 text-right">
                          <button class="text-xs transition-colors cursor-pointer border-0 bg-transparent" style="color: var(--state-error);"
                                  @click="deleteFile(f)"
                                  @mouseenter="$event.target.style.textDecoration='underline'"
                                  @mouseleave="$event.target.style.textDecoration='none'">删除</button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div v-else class="rounded-lg flex flex-col items-center justify-center py-10" style="border: 1px solid var(--color-border-light); background: var(--color-bg-sunken);">
                  <FolderOpen style="width: 32px; height: 32px; color: var(--color-text-tertiary); margin-bottom: 8px;" />
                  <p class="text-sm" style="color: var(--color-text-tertiary);">暂无文件</p>
                </div>

              </div>
            </div>

          </div>
        </div>
        <!-- End Category 1: 项目文件 -->

        <!-- ============================================ -->
        <!-- Category 2: 基础组件                          -->
        <!-- ============================================ -->
        <div class="mb-8">
          <div class="rounded-lg px-6 py-4 mb-4" style="background: var(--color-bg-elevated); border: 1px solid var(--color-border-light); box-shadow: var(--shadow-sm);">
            <div class="flex items-center gap-3 mb-1">
              <div class="flex items-center justify-center rounded-md" style="width: 36px; height: 36px; background: #F0FDF4;">
                <Blocks style="width: 18px; height: 18px; color: var(--state-success);" />
              </div>
              <div>
                <h2 class="font-heading text-base" style="color: var(--color-text-primary);">基础组件</h2>
                <p class="text-xs" style="color: var(--color-text-tertiary);">上传JDK、MySQL便携包、MinIO、Nginx和UnSim引擎等基础组件，支持多版本管理</p>
              </div>
            </div>
          </div>

          <!-- Sub-tabs -->
          <div class="flex items-center gap-1 mb-5 p-1 rounded-lg inline-flex" style="background: var(--color-bg-sunken);">
            <button v-for="tab in infraTabs" :key="tab.key"
                    class="sub-tab-btn px-4 py-2 text-sm rounded-md transition-all cursor-pointer border-0"
                    :class="{ active: activeInfraTab === tab.key }"
                    :style="activeInfraTab === tab.key ? {} : { color: 'var(--color-text-secondary)', background: 'transparent' }"
                    @click="activeInfraTab = tab.key">
              {{ tab.label }}
            </button>
          </div>

          <!-- Tab Content -->
          <template v-for="tab in infraTabs" :key="'tab-' + tab.key">
          <div v-if="activeInfraTab === tab.key">

            <!-- Version list header with upload button -->
            <div class="flex items-center justify-between mb-4">
                <div class="flex items-center gap-2">
                  <span class="text-sm font-medium" style="color: var(--color-text-primary);">{{ tab.label }} 版本列表</span>
                  <span class="text-xs px-2 py-0.5 rounded-full"
                        :style="getInfraFileCount(tab.key) > 0
                          ? { background: 'var(--color-primary-light)', color: 'var(--color-primary)', fontWeight: 500 }
                          : { background: 'var(--color-bg-muted)', color: 'var(--color-text-tertiary)', fontWeight: 500 }">
                    {{ getInfraFileCount(tab.key) }} 个版本
                  </span>
                </div>
              <button class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md transition-colors cursor-pointer border-0"
                      style="background: var(--color-primary); color: #FFF;"
                      @click="toggleInfraUploadZone(tab.key)"
                      @mouseenter="$event.target.style.background='var(--color-primary-hover)'"
                      @mouseleave="$event.target.style.background='var(--color-primary)'">
                <Plus style="width: 14px; height: 14px;" />
                上传新版本
              </button>
            </div>

            <!-- Version Table -->
            <div v-if="getInfraFiles(tab.key).length > 0" class="rounded-lg overflow-hidden" style="background: var(--color-bg-elevated); border: 1px solid var(--color-border-light); box-shadow: var(--shadow-sm);">
              <table class="w-full text-sm" style="border-collapse: collapse;">
                <thead>
                  <tr style="background: var(--color-bg-sunken);">
                    <th class="text-left px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">版本</th>
                    <th class="text-left px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">标签</th>
                    <th v-if="hasInitState(tab.key)" class="text-left px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">初始化状态</th>
                    <th class="text-left px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">文件名</th>
                    <th class="text-left px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">大小</th>
                    <th class="text-left px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">上传时间</th>
                    <th v-if="hasInitState(tab.key)" class="text-left px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">所属系统</th>
                    <th class="text-right px-5 py-2.5 font-medium" style="color: var(--color-text-secondary); font-size: 12px;">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="f in getInfraFiles(tab.key)" :key="f.id" class="table-row-hover" :style="{ borderBottom: '1px solid var(--color-border-light)' }">
                    <td class="px-5 py-3">
                      <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium" style="background: var(--state-info-bg); color: var(--state-info);">
                        {{ f.version || f.fileName }}
                      </span>
                    </td>
                    <td class="px-5 py-3">
                      <span class="text-xs px-2 py-0.5 rounded" style="background: var(--color-bg-sunken); color: var(--color-text-secondary);">
                        {{ f.tag || '-' }}
                      </span>
                    </td>
                    <td v-if="hasInitState(tab.key)" class="px-5 py-3">
                      <span v-if="f.initState === 'clean'" class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium" style="background: #F0FDF4; color: #16A34A;">
                        <Box style="width: 10px; height: 10px;" />纯净版
                      </span>
                      <span v-else class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium" style="background: #EFF6FF; color: #2563EB;">
                        <Settings style="width: 10px; height: 10px;" />已初始化
                      </span>
                    </td>
                    <td class="px-5 py-3">
                      <div class="flex items-center gap-2">
                        <component :is="getInfraIcon(tab.key)" style="width: 16px; height: 16px;" :style="{ color: getInfraIconColor(tab.key) }" />
                        <span style="color: var(--color-text-primary);">{{ f.fileName }}</span>
                      </div>
                    </td>
                    <td class="px-5 py-3" style="color: var(--color-text-secondary);">{{ formatSize(f.fileSize) }}</td>
                    <td class="px-5 py-3" style="color: var(--color-text-secondary);">{{ formatDate(f.createdAt) }}</td>
                    <td v-if="hasInitState(tab.key)" class="px-5 py-3">
                      <span class="text-xs px-2 py-0.5 rounded-full"
                            :style="f.belongsTo === '通用'
                              ? { background: 'var(--color-bg-muted)', color: 'var(--color-text-secondary)' }
                              : { background: 'var(--color-primary-light)', color: 'var(--color-primary-text)' }">
                        {{ f.belongsTo || '通用' }}
                      </span>
                    </td>
                    <td class="px-5 py-3 text-right">
                      <button class="text-xs transition-colors cursor-pointer border-0 bg-transparent mr-3" style="color: var(--state-info);"
                              @click="openDetailModal(f)"
                              @mouseenter="$event.target.style.textDecoration='underline'"
                              @mouseleave="$event.target.style.textDecoration='none'">详情</button>
                      <button class="text-xs transition-colors cursor-pointer border-0 bg-transparent" style="color: var(--state-error);"
                              @click="deleteInfraFile(f)"
                              @mouseenter="$event.target.style.textDecoration='underline'"
                              @mouseleave="$event.target.style.textDecoration='none'">删除</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="rounded-lg flex flex-col items-center justify-center py-10" style="border: 1px solid var(--color-border-light); background: var(--color-bg-sunken);">
              <FolderOpen style="width: 32px; height: 32px; color: var(--color-text-tertiary); margin-bottom: 8px;" />
              <p class="text-sm" style="color: var(--color-text-tertiary);">暂无版本</p>
            </div>

            <!-- Upload Zone (collapsed by default) -->
            <div v-show="infraUploadOpen[tab.key]" class="mt-4">
              <div class="rounded-lg flex flex-col items-center justify-center transition-colors"
                   style="border: 2px dashed var(--color-primary); background: var(--color-primary-light); padding: 32px 24px; min-height: 140px;"
                   :style="infraDragOver ? { borderColor: 'var(--color-primary)', background: 'var(--color-primary-light)', boxShadow: '0 0 0 3px rgba(59,130,246,0.15)' } : {}"
                   @dragover.prevent="infraDragOver = true"
                   @dragleave.prevent="infraDragOver = false"
                   @drop.prevent="handleInfraDrop">
                <div class="mb-3 flex items-center justify-center rounded-full" style="width: 48px; height: 48px; background: var(--color-primary-light);">
                  <component :is="getUploadIcon(tab.key)" style="width: 24px; height: 24px; color: var(--color-primary);" />
                </div>
                <p class="text-sm mb-1" style="color: var(--color-text-primary); font-weight: 500;">
                  {{ getUploadPrompt(tab.key) }}
                </p>
                <p class="text-xs mb-3" style="color: var(--color-text-tertiary);">
                  {{ getUploadHint(tab.key) }}
                </p>
                <div class="flex items-center gap-3 mb-3 flex-wrap justify-center">
                  <div class="flex items-center gap-1.5">
                    <label class="text-xs whitespace-nowrap" style="color: var(--color-text-secondary);">版本:<span style="color: var(--state-error);">*</span></label>
                    <input type="text" v-model="infraUploadForm.version" :placeholder="getVersionPlaceholder(tab.key)" class="text-xs px-2.5 py-1.5 rounded-md outline-none"
                           :style="{ width: getVersionInputWidth(tab.key), border: '1px solid var(--color-border)', background: 'var(--color-bg-elevated)', color: 'var(--color-text-primary)' }">
                  </div>
                  <div class="flex items-center gap-1.5">
                    <label class="text-xs whitespace-nowrap" style="color: var(--color-text-secondary);">标签:</label>
                    <input type="text" v-model="infraUploadForm.tag" :placeholder="getTagPlaceholder(tab.key)" class="text-xs px-2.5 py-1.5 rounded-md outline-none"
                           :style="{ width: getTagInputWidth(tab.key), border: '1px solid var(--color-border)', background: 'var(--color-bg-elevated)', color: 'var(--color-text-primary)' }">
                  </div>
                  <div v-if="hasInitState(tab.key)" class="flex items-center gap-1.5">
                    <label class="text-xs whitespace-nowrap" style="color: var(--color-text-secondary);">初始化状态:</label>
                    <select v-model="infraUploadForm.initState" class="text-xs px-2.5 py-1.5 rounded-md outline-none"
                            style="border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-primary);">
                      <option value="clean">纯净版</option>
                      <option value="initialized">含初始化数据</option>
                    </select>
                  </div>
                  <div v-if="hasInitState(tab.key)" class="flex items-center gap-1.5">
                    <label class="text-xs whitespace-nowrap" style="color: var(--color-text-secondary);">所属系统:</label>
                    <select v-model="infraUploadForm.belongsTo" class="text-xs px-2.5 py-1.5 rounded-md outline-none"
                            style="border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-primary);">
                      <option value="通用">通用</option>
                      <option v-for="p in projectList" :key="p.id" :value="p.name">{{ p.name }}</option>
                    </select>
                  </div>
                </div>
                <button class="px-4 py-2 text-sm rounded-md transition-colors cursor-pointer border-0"
                        style="background: var(--color-primary); color: #FFF;"
                        @click="startInfraUpload(tab.key)"
                        @mouseenter="$event.target.style.background='var(--color-primary-hover)'"
                        @mouseleave="$event.target.style.background='var(--color-primary)'">
                  选择压缩包
                </button>
                <p class="text-xs mt-2" style="color: var(--color-text-tertiary);">
                  {{ getUploadTip(tab.key) }}
                </p>
              </div>
            </div>
          </div>
          </template>
        </div>


    <!-- Hidden input for infra upload (archive only) -->
    <input type="file" ref="infraFileRef" style="display: none;"
           accept=".zip,.tar,.tar.gz,.tgz,.7z"
           @change="handleInfraFileSelected($event)">

    <!-- Detail Modal -->
    <div v-show="detailModalVisible" class="detail-modal-overlay" @click.self="closeDetailModal">
      <div class="detail-modal">
        <div class="detail-modal-header">
          <h3>组件详情</h3>
          <button class="detail-modal-close" @click="closeDetailModal">
            <X style="width: 18px; height: 18px;" />
          </button>
        </div>
        <div class="detail-modal-body">
          <div class="detail-form-row">
            <span class="detail-form-label">文件名</span>
            <input type="text" :value="detailFile?.fileName" readonly class="detail-form-input">
          </div>
          <div class="detail-form-row">
            <span class="detail-form-label">版本</span>
            <input type="text" v-model="detailEditForm.version" class="detail-form-input" placeholder="版本号">
          </div>
          <div class="detail-form-row">
            <span class="detail-form-label">标签</span>
            <input type="text" v-model="detailEditForm.tag" class="detail-form-input" placeholder="标签">
          </div>
          <div v-if="detailFile?.initState || hasInitState(activeInfraTab)" class="detail-form-row">
            <span class="detail-form-label">初始化状态</span>
            <select v-model="detailEditForm.initState" class="detail-form-input">
              <option value="clean">纯净版</option>
              <option value="initialized">含初始化数据</option>
            </select>
          </div>
          <div class="detail-form-row">
            <span class="detail-form-label">大小</span>
            <input type="text" :value="formatSize(detailFile?.fileSize)" readonly class="detail-form-input">
          </div>
          <div class="detail-form-row">
            <span class="detail-form-label">上传时间</span>
            <input type="text" :value="formatDate(detailFile?.createdAt)" readonly class="detail-form-input">
          </div>
          <div v-if="detailFile?.belongsTo || hasInitState(activeInfraTab)" class="detail-form-row">
            <span class="detail-form-label">所属系统</span>
            <select v-model="detailEditForm.belongsTo" class="detail-form-input">
              <option value="通用">通用</option>
              <option v-for="p in projectList" :key="p.id" :value="p.name">{{ p.name }}</option>
            </select>
          </div>
        </div>
        <div class="detail-modal-footer">
          <button class="px-4 py-2 rounded-lg text-sm font-medium cursor-pointer"
                  style="border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-secondary);"
                  @click="closeDetailModal">
            取消
          </button>
          <button class="px-4 py-2 rounded-lg text-sm font-medium cursor-pointer border-0"
                  style="background: var(--color-primary); color: var(--color-text-inverse);"
                  @click="saveDetail">
            保存
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import {
  FolderOpen, Folder, FolderTree, ChevronRight, UploadCloud, FolderUp,
  AlertTriangle, FileArchive, Blocks, Plus, Box, Settings,
  Package, HardDrive, Database, Globe, Cog, X, FileBox
} from 'lucide-vue-next'
import { getProjectList } from '@/api/project'
import { listFiles, uploadFile, uploadFolder, deleteFile as deleteFileApi, uploadLargeFile } from '@/api/file'
import { getInfraList, uploadInfraZip, deleteInfraFile as deleteInfraFileApi, editInfraFile } from '@/api/infrastructure'
import { formatFileSize, formatDateTime } from '@/utils/format'

const route = useRoute()

const uploadHover = ref(false)
const dragOver = ref(false)
const infraDragOver = ref(false)
const uploadVersionTag = ref('')
const fileInputRef = ref(null)
const activeInfraTab = ref('jdk')
const detailModalVisible = ref(false)
const detailFile = ref(null)
const detailEditForm = reactive({
  version: '',
  tag: '',
  initState: 'clean',
  belongsTo: '通用'
})

const infraTabs = [
  { key: 'jdk', label: 'JDK' },
  { key: 'mysql', label: 'MySQL' },
  { key: 'minio', label: 'MinIO' },
  { key: 'nginx', label: 'Nginx' },
  { key: 'redis', label: 'Redis' },
  { key: 'engine', label: 'UnSim引擎' }
]

const infraUploadOpen = reactive({
  jdk: false, mysql: false, minio: false, nginx: false, redis: false, engine: false
})

const infraUploadForm = reactive({
  version: '',
  tag: '',
  initState: 'clean',
  belongsTo: '通用'
})

const projectList = ref([])
const projectTree = ref([])
const selectedProjectId = ref(null)
const selectedFileType = ref('jar') // jar or vue
const fileMap = reactive({}) // { [projectId]: { jar: [], vue: [] } }
const infraFileMap = reactive({
  jdk: [], mysql: [], minio: [], nginx: [], redis: [], engine: []
})

const selectedProjectName = computed(() => {
  const p = projectList.value.find(x => x.id === selectedProjectId.value)
  return p ? p.name : ''
})

const currentFiles = computed(() => {
  if (!selectedProjectId.value) return []
  const map = fileMap[selectedProjectId.value]
  if (!map) return []
  return map[selectedFileType.value] || []
})

function toggleProject(p) {
  p.expanded = !p.expanded
}

function selectProjectFile(p, type) {
  selectedProjectId.value = p.id
  selectedFileType.value = type
  if (!p.expanded) p.expanded = true
  loadProjectFiles(p.id, type)
}

function isProjectFileSelected(pid, type) {
  return selectedProjectId.value === pid && selectedFileType.value === type
}

function getJarCount(pid) {
  const m = fileMap[pid]
  return m ? (m.jar || []).length : 0
}

function getVueCount(pid) {
  const m = fileMap[pid]
  return m ? (m.vue || []).length : 0
}

async function loadProjects() {
  try {
    const res = await getProjectList()
    const list = res?.records || res || []
    projectList.value = list
    projectTree.value = list.map(p => ({
      id: p.id,
      name: p.name,
      expanded: false
    }))
    if (projectTree.value.length > 0) {
      projectTree.value[0].expanded = true
      selectedProjectId.value = projectTree.value[0].id
      await loadProjectFiles(selectedProjectId.value, 'jar')
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadProjectFiles(pid, type) {
  if (!fileMap[pid]) fileMap[pid] = { jar: [], vue: [] }
  if (fileMap[pid][type] && fileMap[pid][type].length > 0) return
  try {
    const res = await listFiles({ projectId: pid, type })
    fileMap[pid][type] = res?.records || res || []
  } catch (e) {
    console.error(e)
  }
}

const uploading = ref(false)
const infraUploading = ref(false)

function handleUploadClick() {
  if (fileInputRef.value) {
    fileInputRef.value.click()
  }
}

function handleDrop(e) {
  dragOver.value = false
  const files = e.dataTransfer?.files
  if (!files || files.length === 0) return
  if (!selectedProjectId.value) {
    ElMessage.warning('请先选择项目')
    return
  }
  const fakeEvent = { target: { files, value: '' } }
  handleFileSelected(fakeEvent)
}

async function handleFileSelected(e) {
  const files = e.target.files
  if (!files || files.length === 0) return
  if (!selectedProjectId.value) {
    ElMessage.warning('请先选择项目')
    e.target.value = ''
    return
  }
  let loading = null
  try {
    uploading.value = true
    loading = ElLoading.service({
      lock: true,
      text: selectedFileType.value === 'jar' ? '正在上传JAR包...' : '正在上传Vue前端...',
      background: 'rgba(0, 0, 0, 0.5)'
    })
    if (selectedFileType.value === 'jar') {
      const file = files[0]
      const LARGE_FILE_THRESHOLD = 100 * 1024 * 1024 // 100MB
      const useChunked = file.size > LARGE_FILE_THRESHOLD
      let res
      if (useChunked) {
        loading.setText(`正在分片上传JAR包 (0%)...`)
        res = await uploadLargeFile(file, {
          projectId: selectedProjectId.value,
          type: 'jar',
          versionTag: uploadVersionTag.value || undefined
        }, (progress) => {
          loading.setText(`正在分片上传JAR包 (${progress}%)...`)
        })
      } else {
        const formData = new FormData()
        formData.append('file', file)
        formData.append('projectId', selectedProjectId.value)
        formData.append('type', 'jar')
        if (uploadVersionTag.value) formData.append('versionTag', uploadVersionTag.value)
        res = await uploadFile(formData)
      }
      if (res?.data || res) {
        ElMessage.success(useChunked ? '分片上传成功' : '上传成功')
        uploadVersionTag.value = ''
        fileMap[selectedProjectId.value] = { jar: [], vue: [] }
        loadProjectFiles(selectedProjectId.value, selectedFileType.value)
      }
    } else {
      const fileList = Array.from(files)
      let folderName = 'dist'
      if (fileList.length > 0) {
        const firstPath = fileList[0].webkitRelativePath || fileList[0].name
        const parts = firstPath.split('/')
        if (parts.length > 1) folderName = parts[0]
      }
      const formData = new FormData()
      fileList.forEach(f => formData.append('file', f))
      formData.append('projectId', selectedProjectId.value)
      formData.append('type', 'vue')
      formData.append('folderName', folderName)
      if (uploadVersionTag.value) formData.append('versionTag', uploadVersionTag.value)
      const res = await uploadFolder(formData)
      if (res?.data || res) {
        ElMessage.success('上传成功')
        uploadVersionTag.value = ''
        fileMap[selectedProjectId.value] = { jar: [], vue: [] }
        loadProjectFiles(selectedProjectId.value, selectedFileType.value)
      }
    }
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '上传失败')
  } finally {
    uploading.value = false
    e.target.value = ''
    if (loading) loading.close()
  }
}

async function deleteFile(f) {
  try {
    await ElMessageBox.confirm('确认删除该文件？', '提示', { type: 'warning' })
    await deleteFileApi(f.id)
    ElMessage.success('删除成功')
    fileMap[selectedProjectId.value] = { jar: [], vue: [] }
    loadProjectFiles(selectedProjectId.value, selectedFileType.value)
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

function toggleInfraUploadZone(key) {
  infraUploadOpen[key] = !infraUploadOpen[key]
}

function hasInitState(key) {
  return key === 'mysql' || key === 'minio'
}

function getInfraIcon(key) {
  switch (key) {
    case 'jdk': return HardDrive
    case 'mysql': return Database
    case 'minio': return HardDrive
    case 'nginx': return Globe
    case 'redis': return Database
    case 'engine': return Cog
    default: return Package
  }
}

function getInfraIconColor(key) {
  switch (key) {
    case 'jdk': return 'var(--state-success)'
    case 'mysql': return 'var(--state-warning)'
    case 'minio': return 'var(--state-success)'
    case 'nginx': return 'var(--state-warning)'
    case 'redis': return 'var(--state-error)'
    case 'engine': return 'var(--color-primary)'
    default: return 'var(--state-info)'
  }
}

function getInfraFiles(key) {
  return infraFileMap[key] || []
}

function getInfraFileCount(key) {
  return getInfraFiles(key).length
}

function getUploadIcon(key) {
  return FileArchive
}

function getUploadPrompt(key) {
  const labels = { jdk: 'JDK', mysql: 'MySQL', minio: 'MinIO', nginx: 'Nginx', redis: 'Redis', engine: 'UnSim引擎' }
  return `拖拽${labels[key] || ''}压缩包到此处`
}

function getUploadHint(key) {
  return '支持 zip / tar / tar.gz / 7z 格式'
}

function getVersionPlaceholder(key) {
  switch (key) {
    case 'jdk': return '17.0.2'
    case 'mysql': return '8.0.35'
    case 'minio': return '2024.06'
    case 'nginx': return '1.24'
    case 'redis': return '7.2.0'
    case 'engine': return 'v3.2'
    default: return '版本号'
  }
}

function getTagPlaceholder(key) {
  switch (key) {
    case 'jdk': return '如: 推荐版本'
    case 'mysql': return '如: 含电商初始化数据'
    case 'minio': return '如: 含电商Bucket策略'
    case 'nginx': return '如: 稳定版'
    case 'redis': return '如: 含配置模板'
    case 'engine': return '电商平台专用引擎'
    default: return '如: 推荐版本'
  }
}

function getVersionInputWidth(key) {
  switch (key) {
    case 'jdk': return '100px'
    case 'mysql': return '100px'
    case 'minio': return '100px'
    case 'nginx': return '100px'
    case 'redis': return '100px'
    case 'engine': return '80px'
    default: return '100px'
  }
}

function getTagInputWidth(key) {
  switch (key) {
    case 'jdk': return '100px'
    case 'mysql': return '140px'
    case 'minio': return '140px'
    case 'nginx': return '100px'
    case 'redis': return '120px'
    case 'engine': return '140px'
    default: return '100px'
  }
}

function getUploadTip(key) {
  const labels = { jdk: 'JDK', mysql: 'MySQL', minio: 'MinIO', nginx: 'Nginx', redis: 'Redis', engine: 'UnSim引擎' }
  const name = labels[key] || '组件'
  if (key === 'mysql' || key === 'minio') {
    return `${name}：纯净版由系统注入项目配置中的端口和账密 | 含初始化数据：包内预置数据，启动后直接使用`
  }
  return `${name} 仅支持上传纯净版，运行时由启动脚本自动注入配置`
}

const infraFileRef = ref(null)

async function loadInfraFiles(key) {
  if (infraFileMap[key] && infraFileMap[key].length > 0) return
  try {
    const res = await getInfraList(key)
    infraFileMap[key] = res?.records || res?.data || res || []
  } catch (e) {
    console.error(e)
  }
}

function startInfraUpload(key) {
  if (!infraUploadForm.version) {
    ElMessage.warning('请输入版本号')
    return
  }
  if (infraFileRef.value) infraFileRef.value.click()
}

function handleInfraDrop(e) {
  infraDragOver.value = false
  const files = e.dataTransfer?.files
  if (!files || files.length === 0) return
  const type = activeInfraTab.value
  if (!infraUploadForm.version) {
    ElMessage.warning('请先输入版本号')
    return
  }
  // 校验压缩包格式
  const file = files[0]
  if (!isValidArchive(file.name)) {
    ElMessage.warning('仅支持 zip / tar / tar.gz / 7z 格式的压缩包')
    return
  }
  const fakeEvent = { target: { files: [file], value: '' } }
  handleInfraFileSelected(fakeEvent)
}

function isValidArchive(fileName) {
  const lower = fileName.toLowerCase()
  return lower.endsWith('.zip') || lower.endsWith('.tar') || lower.endsWith('.tar.gz') || lower.endsWith('.tgz') || lower.endsWith('.7z')
}

async function handleInfraFileSelected(e) {
  const files = e.target.files
  if (!files || files.length === 0) return
  const type = activeInfraTab.value
  const file = files[0]
  if (!isValidArchive(file.name)) {
    ElMessage.warning('仅支持 zip / tar / tar.gz / 7z 格式的压缩包')
    e.target.value = ''
    return
  }
  let loading = null
  try {
    infraUploading.value = true
    loading = ElLoading.service({
      lock: true,
      text: `正在上传并解压${infraTabs.find(t => t.key === type)?.label || '组件'}...`,
      background: 'rgba(0, 0, 0, 0.5)'
    })
    const params = {
      version: infraUploadForm.version,
      tag: infraUploadForm.tag,
      initState: infraUploadForm.initState,
      belongsTo: infraUploadForm.belongsTo
    }
    const res = await uploadInfraZip(type, file, params)
    if (res?.data || res) {
      ElMessage.success('压缩包上传并解压成功')
      infraUploadForm.version = ''
      infraUploadForm.tag = ''
      infraUploadForm.initState = 'clean'
      infraUploadForm.belongsTo = '通用'
      infraUploadOpen[type] = false
      const newFile = res.data || res
      if (!infraFileMap[type]) infraFileMap[type] = []
      infraFileMap[type].unshift(newFile)
    }
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '上传失败')
  } finally {
    infraUploading.value = false
    e.target.value = ''
    if (loading) loading.close()
  }
}

function openDetailModal(f) {
  detailFile.value = f
  detailEditForm.version = f.version || ''
  detailEditForm.tag = f.tag || ''
  detailEditForm.initState = f.initState || 'clean'
  detailEditForm.belongsTo = f.belongsTo || '通用'
  detailModalVisible.value = true
}

function closeDetailModal() {
  detailModalVisible.value = false
}

async function saveDetail() {
  if (!detailFile.value) return
  if (!detailEditForm.version || detailEditForm.version.trim() === '') {
    ElMessage.warning('版本号不能为空')
    return
  }
  try {
    const type = activeInfraTab.value
    await editInfraFile(type, detailFile.value.id, {
      version: detailEditForm.version,
      tag: detailEditForm.tag,
      initState: detailEditForm.initState,
      belongsTo: detailEditForm.belongsTo
    })
    ElMessage.success('保存成功')
    const file = infraFileMap[type]
    if (file) {
      const idx = file.findIndex(x => x.id === detailFile.value.id)
      if (idx >= 0) {
        file[idx] = { ...file[idx], ...detailEditForm }
      }
    }
    detailModalVisible.value = false
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '保存失败')
  }
}

async function deleteInfraFile(f) {
  try {
    await ElMessageBox.confirm('确认删除该组件？', '提示', { type: 'warning' })
    const type = activeInfraTab.value
    await deleteInfraFileApi(type, f.id)
    ElMessage.success('删除成功')
    if (infraFileMap[type]) {
      infraFileMap[type] = infraFileMap[type].filter(x => x.id !== f.id)
    }
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

function formatSize(size) { return formatFileSize(size) }

function formatDate(t) {
  if (!t) return ''
  const d = new Date(t)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

watch(activeInfraTab, (newVal) => {
  loadInfraFiles(newVal)
  infraUploadForm.version = ''
  infraUploadForm.tag = ''
  infraUploadForm.initState = 'clean'
  infraUploadForm.belongsTo = '通用'
  // 关闭所有展开的上传面板
  Object.keys(infraUploadOpen).forEach(k => { infraUploadOpen[k] = false })
})

onMounted(() => {
  loadProjects()
  loadInfraFiles(activeInfraTab.value)
})
</script>

<style scoped>
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }

.table-row-hover:hover { background: var(--color-bg-sunken); }
.table-row-hover { transition: background 150ms ease; }

.sub-tab-btn.active {
  background: var(--color-bg-elevated) !important;
  color: var(--color-primary) !important;
  box-shadow: var(--shadow-sm);
  font-weight: 500;
}
.sub-tab-btn { cursor: pointer; border: none; }
.sub-tab-btn:not(.active):hover { background: var(--color-bg-muted); }

/* Tree styles */
.tree-node {
  cursor: pointer;
  user-select: none;
  display: flex;
  align-items: center;
  padding: 5px 8px;
  border-radius: 4px;
  transition: background 100ms ease, color 100ms ease;
  gap: 4px;
}
.tree-node:hover { background: var(--color-bg-sunken); }
.tree-node.selected { background: var(--color-primary-light); color: var(--color-primary); }
.tree-node.selected .tree-chevron { color: var(--color-primary); }
.tree-node.selected .tree-folder-icon { color: var(--color-primary); }
.tree-chevron {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 150ms ease;
  color: var(--color-text-tertiary);
  width: 16px;
  height: 16px;
}
.tree-chevron.expanded { transform: rotate(90deg); }
.tree-folder-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--color-text-tertiary);
  width: 16px;
  height: 16px;
}
.tree-node.selected .tree-folder-icon { color: var(--color-primary); }
.tree-label {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--color-text-primary);
}
.tree-node.selected .tree-label { color: var(--color-primary); font-weight: 500; }
.tree-children { overflow: hidden; }
.tree-children.collapsed { display: none; }

/* Detail modal */
.detail-modal-overlay {
  display: flex;
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 1000;
  align-items: center;
  justify-content: center;
}
.detail-modal {
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  box-shadow: 0 24px 48px rgba(0,0,0,0.2);
  width: 90%;
  max-width: 560px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.detail-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-light);
}
.detail-modal-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}
.detail-modal-close {
  width: 32px; height: 32px;
  border-radius: var(--radius-sm);
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  transition: background 100ms;
}
.detail-modal-close:hover {
  background: var(--color-bg-sunken);
  color: var(--color-text-primary);
}
.detail-modal-body {
  padding: 20px;
  flex: 1;
  overflow-y: auto;
}
.detail-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 20px;
  border-top: 1px solid var(--color-border-light);
}
.detail-form-input:not([readonly]) {
  cursor: text;
}
select.detail-form-input {
  cursor: pointer;
  appearance: auto;
}
.detail-form-row {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}
.detail-form-row:last-child { margin-bottom: 0; }
.detail-form-label {
  flex-shrink: 0;
  width: 80px;
  font-size: 13px;
  color: var(--color-text-secondary);
  text-align: right;
}
.detail-form-input {
  flex: 1;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-bg-elevated);
  color: var(--color-text-primary);
  font-size: 13px;
  outline: none;
}
.detail-form-input[readonly] {
  background: var(--color-bg-sunken);
  color: var(--color-text-secondary);
  cursor: default;
}
</style>
