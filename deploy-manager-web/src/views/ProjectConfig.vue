<template>
  <main class="flex min-h-screen" style="background: var(--color-bg);">
    <AppSidebar />
    <div class="flex-1 flex flex-col overflow-hidden">
      <AppHeader :breadcrumbs="breadcrumbList" />
      <div class="flex-1 overflow-y-auto p-8" style="background: var(--color-bg);">

        <div class="mb-8">
          <h1 class="font-heading text-2xl mb-1" style="color: var(--color-text-primary);">{{ projectName }} - 项目配置</h1>
          <p class="text-sm" style="color: var(--color-text-secondary);">配置项目打包所需的JAR包、前端产物和基础组件</p>
        </div>

        <!-- Section A: 项目文件 -->
        <div class="rounded-lg p-6 mb-6" style="background: var(--color-bg-elevated); border: 1px solid var(--color-border-light); box-shadow: var(--shadow-sm);">
          <div class="flex items-center gap-3 mb-6">
            <div class="flex items-center justify-center rounded-md" style="width: 36px; height: 36px; background: var(--color-primary-light);">
              <FolderOpen style="width: 18px; height: 18px; color: var(--color-primary);" />
            </div>
            <h2 class="font-heading text-base" style="color: var(--color-text-primary);">项目文件</h2>
          </div>

          <div class="flex flex-col gap-6">
            <!-- JAR 包 + 应用配置 -->
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-medium" style="color: var(--color-text-secondary);">JAR包</label>
              <div class="flex items-center gap-3">
                <div class="path-dropdown" style="max-width: 520px; flex: 1;" :class="{ 'is-open': jarDropdownOpen }">
                  <div class="path-dropdown-trigger" @click="toggleJarDropdown">
                    <span style="display: flex; flex-direction: column; gap: 2px;">
                      <span style="font-size: 11px; color: var(--color-text-tertiary); font-family: var(--font-mono);">{{ jarPathLabel }}</span>
                      <span style="font-size: 13px; color: var(--color-text-primary);">{{ jarDisplayLabel }}</span>
                    </span>
                    <ChevronDown style="width: 14px; height: 14px; color: var(--color-text-tertiary); flex-shrink: 0;" />
                  </div>
                  <div class="path-dropdown-panel" v-show="jarDropdownOpen">
                    <div class="path-group" v-for="(group, gIdx) in jarGrouped" :key="gIdx">
                      <div class="path-group-label">{{ group.label }}</div>
                      <div
                        class="path-item"
                        :class="{ selected: form.jarFileId === f.id }"
                        v-for="f in group.items"
                        :key="f.id"
                        @click="selectJar(f)"
                      >
                        <FileArchive style="width: 14px; height: 14px;" class="file-icon" />
                        {{ f.fileName }}
                        <span class="file-size">{{ formatSize(f.fileSize) }}</span>
                      </div>
                    </div>
                    <div v-if="jarOptions.length === 0" class="path-item" style="color: var(--color-text-tertiary); justify-content: center;">
                      暂无文件，请先上传
                    </div>
                  </div>
                </div>
              </div>
              <a class="text-sm font-medium no-underline cursor-pointer" style="color: var(--color-primary); display: inline-flex; align-items: center; gap: 4px; margin-top: 4px;"
                 @click="goToUpload('jar')">
                <ExternalLink style="width: 12px; height: 12px;" />
                管理文件
              </a>
              <div class="flex items-center gap-4 mt-3" style="padding: 12px 16px; background: var(--color-bg-sunken); border-radius: var(--radius-md);">
                <div class="flex items-center gap-2">
                  <label class="text-xs font-medium" style="color: var(--color-text-tertiary); white-space: nowrap;">应用端口</label>
                  <input type="text" v-model.number="form.appPort" class="px-3 py-1.5 rounded-md text-sm outline-none" style="width: 80px; background: var(--color-bg-elevated); border: 1px solid var(--color-border); color: var(--color-text-primary); text-align: center; font-family: var(--font-mono);" />
                </div>
                <div style="width: 1px; height: 24px; background: var(--color-border-light);"></div>
                <div class="flex items-center gap-2 flex-1">
                  <label class="text-xs font-medium" style="color: var(--color-text-tertiary); white-space: nowrap;">JVM参数</label>
                  <input type="text" v-model="form.jvmParams" class="px-3 py-1.5 rounded-md text-sm outline-none flex-1" style="background: var(--color-bg-elevated); border: 1px solid var(--color-border); color: var(--color-text-primary); font-family: var(--font-mono);" />
                </div>
              </div>
            </div>

            <div style="border-top: 1px solid var(--color-border-light); margin: 0;"></div>

            <!-- Vue 前端产物 -->
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-medium" style="color: var(--color-text-secondary);">Vue前端产物</label>
              <div class="flex items-center gap-3">
                <div class="path-dropdown" style="max-width: 520px; flex: 1;" :class="{ 'is-open': vueDropdownOpen }">
                  <div class="path-dropdown-trigger" @click="toggleVueDropdown">
                    <span style="display: flex; flex-direction: column; gap: 2px;">
                      <span style="font-size: 11px; color: var(--color-text-tertiary); font-family: var(--font-mono);">{{ vuePathLabel }}</span>
                      <span style="font-size: 13px; color: var(--color-text-primary);">{{ vueDisplayLabel }}</span>
                    </span>
                    <ChevronDown style="width: 14px; height: 14px; color: var(--color-text-tertiary); flex-shrink: 0;" />
                  </div>
                  <div class="path-dropdown-panel" v-show="vueDropdownOpen">
                    <div class="path-group" v-for="(group, gIdx) in vueGrouped" :key="gIdx">
                      <div class="path-group-label">{{ group.label }}</div>
                      <div
                        class="path-item"
                        :class="{ selected: form.vueFolderId === f.id }"
                        v-for="f in group.items"
                        :key="f.id"
                        @click="selectVue(f)"
                      >
                        <Folder style="width: 14px; height: 14px;" class="file-icon" />
                        {{ f.fileName }}
                        <span class="file-size">{{ formatSize(f.fileSize) }}</span>
                      </div>
                    </div>
                    <div v-if="vueOptions.length === 0" class="path-item" style="color: var(--color-text-tertiary); justify-content: center;">
                      暂无文件，请先上传
                    </div>
                  </div>
                </div>
              </div>
              <a class="text-sm font-medium no-underline cursor-pointer" style="color: var(--color-primary); display: inline-flex; align-items: center; gap: 4px; margin-top: 4px;"
                 @click="goToUpload('vue')">
                <ExternalLink style="width: 12px; height: 12px;" />
                管理文件
              </a>
            </div>
          </div>
        </div>

        <!-- Section B: 基础组件（版本选择） -->
        <div class="rounded-lg p-6 mb-6" style="background: var(--color-bg-elevated); border: 1px solid var(--color-border-light); box-shadow: var(--shadow-sm);">
          <div class="flex items-center gap-3 mb-2">
            <div class="flex items-center justify-center rounded-md" style="width: 36px; height: 36px; background: #F0FDF4;">
              <Blocks style="width: 18px; height: 18px; color: var(--state-success);" />
            </div>
            <div>
              <h2 class="font-heading text-base" style="color: var(--color-text-primary);">基础组件（版本选择）</h2>
              <p class="text-xs" style="color: var(--color-text-tertiary);">选择每个组件的版本，打包时将包含所选版本</p>
            </div>
          </div>

          <div style="margin-top: 16px;">
            <!-- JDK -->
            <div class="req-row">
              <div class="req-row-icon" style="background: #FFFBEB;">
                <Coffee style="width: 18px; height: 18px; color: var(--state-warning);" />
              </div>
              <div class="req-row-body">
                <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                  <span class="req-row-name">JDK</span>
                  <div class="path-dropdown" id="dd-jdk" style="min-width: 260px;" :class="{ 'is-open': jdkDropdownOpen }">
                    <div class="path-dropdown-trigger" @click="toggleJdkDropdown">
                      <span>{{ jdkDisplayLabel }}</span>
                      <ChevronDown style="width: 14px; height: 14px; color: var(--color-text-tertiary); flex-shrink: 0;" />
                    </div>
                    <div class="path-dropdown-panel" v-show="jdkDropdownOpen">
                      <div class="path-group">
                        <div class="path-group-label">JDK 版本</div>
                        <div
                          class="path-item"
                          :class="{ selected: form.jdkComponentId === v.id }"
                          v-for="v in jdkVersions"
                          :key="v.id"
                          @click="selectJdk(v)"
                        >
                          <Box style="width: 14px; height: 14px;" class="file-icon" />
                          <div class="flex flex-col" style="gap: 2px;">
                            <span class="text-xs" style="color: var(--color-text-primary);">{{ v.name }}</span>
                            <span class="text-xs" style="color: var(--color-text-tertiary);">
                              {{ v.label }} · 通用 · {{ v.size }} ·
                              <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #16A34A; margin-right: 4px;"></span>纯净版
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- MySQL -->
            <div class="req-row">
              <div class="req-row-icon" style="background: #F0FDF4;">
                <Database style="width: 18px; height: 18px; color: var(--state-success);" />
              </div>
              <div class="req-row-body">
                <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                  <span class="req-row-name">MySQL</span>
                  <div class="path-dropdown" style="min-width: 280px;" :class="{ 'is-open': mysqlDropdownOpen }">
                    <div class="path-dropdown-trigger" @click="toggleMysqlDropdown">
                      <span>{{ mysqlDisplayLabel }}</span>
                      <ChevronDown style="width: 14px; height: 14px; color: var(--color-text-tertiary); flex-shrink: 0;" />
                    </div>
                    <div class="path-dropdown-panel" v-show="mysqlDropdownOpen">
                      <div class="path-group" v-if="mysqlCommonVersions.length > 0">
                        <div class="path-group-label">通用版本</div>
                        <div
                          class="path-item"
                          :class="{ selected: form.mysqlComponentId === v.id }"
                          v-for="v in mysqlCommonVersions"
                          :key="'clean-' + v.id"
                          @click="selectMysql(v, 'clean')"
                        >
                          <Database style="width: 14px; height: 14px;" class="file-icon" />
                          <div class="flex flex-col" style="gap: 2px;">
                            <span class="text-xs" style="color: var(--color-text-primary);">{{ v.name }}</span>
                            <span class="text-xs" style="color: var(--color-text-tertiary);">
                              {{ v.label }} · 通用 · {{ v.size }} ·
                              <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #16A34A; margin-right: 4px;"></span>纯净版
                            </span>
                          </div>
                        </div>
                      </div>
                      <div class="path-group" v-if="mysqlProjectVersions.length > 0">
                        <div class="path-group-label">{{ projectName }}专属</div>
                        <div
                          class="path-item"
                          :class="{ selected: form.mysqlComponentId === v.id }"
                          v-for="v in mysqlProjectVersions"
                          :key="'cfg-' + v.id"
                          @click="selectMysql(v, 'configured')"
                        >
                          <Database style="width: 14px; height: 14px;" class="file-icon" />
                          <div class="flex flex-col" style="gap: 2px;">
                            <span class="text-xs" style="color: var(--color-text-primary);">{{ v.name }}</span>
                            <span class="text-xs" style="color: var(--color-text-tertiary);">
                              含{{ projectName }}初始化数据 · {{ projectName }} · {{ v.configuredSize }} ·
                              <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #2563EB; margin-right: 4px;"></span>已初始化
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-if="form.mysqlConfigState === 'clean'" class="flex items-center gap-4 mt-3" style="padding: 8px 12px; background: var(--color-bg-sunken); border-radius: var(--radius-md);">
                  <div class="flex items-center gap-2">
                    <label class="text-xs" style="color: var(--color-text-tertiary);">端口:</label>
                    <input type="text" v-model.number="form.mysqlPort" class="text-xs px-2.5 py-1.5 rounded-md" style="border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-primary); outline: none; width: 70px; font-family: var(--font-mono);">
                  </div>
                  <div style="width: 1px; height: 24px; background: var(--color-border-light);"></div>
                  <div class="flex items-center gap-2">
                    <label class="text-xs" style="color: var(--color-text-tertiary);">账号:</label>
                    <input type="text" v-model="form.dbUsername" class="text-xs px-2.5 py-1.5 rounded-md" style="border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-primary); outline: none; width: 80px;">
                  </div>
                  <div class="flex items-center gap-2">
                    <label class="text-xs" style="color: var(--color-text-tertiary);">密码:</label>
                    <input type="password" v-model="form.dbPassword" class="text-xs px-2.5 py-1.5 rounded-md" style="border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-primary); outline: none; width: 100px;">
                  </div>
                </div>
                <div v-else class="flex items-center gap-2 mt-3" style="padding: 8px 12px; background: var(--state-info-bg); border-radius: var(--radius-md);">
                  <div class="flex items-center gap-1.5 text-xs" style="color: var(--state-info);">
                    <Info style="width: 12px; height: 12px;" />
                    <span>使用包内已有配置（端口、账密），系统不再覆盖</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- MinIO -->
            <div class="req-row">
              <div class="req-row-icon" style="background: var(--color-primary-light);">
                <HardDrive style="width: 18px; height: 18px; color: var(--color-primary);" />
              </div>
              <div class="req-row-body">
                <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                  <span class="req-row-name">MinIO</span>
                  <div class="path-dropdown" style="min-width: 280px;" :class="{ 'is-open': minioDropdownOpen }">
                    <div class="path-dropdown-trigger" @click="toggleMinioDropdown">
                      <span>{{ minioDisplayLabel }}</span>
                      <ChevronDown style="width: 14px; height: 14px; color: var(--color-text-tertiary); flex-shrink: 0;" />
                    </div>
                    <div class="path-dropdown-panel" v-show="minioDropdownOpen">
                      <div class="path-group" v-if="minioCommonVersions.length > 0">
                        <div class="path-group-label">通用版本</div>
                        <div
                          class="path-item"
                          :class="{ selected: form.minioComponentId === v.id }"
                          v-for="v in minioCommonVersions"
                          :key="'clean-' + v.id"
                          @click="selectMinio(v, 'clean')"
                        >
                          <HardDrive style="width: 14px; height: 14px;" class="file-icon" />
                          <div class="flex flex-col" style="gap: 2px;">
                            <span class="text-xs" style="color: var(--color-text-primary);">{{ v.name }}</span>
                            <span class="text-xs" style="color: var(--color-text-tertiary);">
                              {{ v.label }} · 通用 · {{ v.size }} ·
                              <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #16A34A; margin-right: 4px;"></span>纯净版
                            </span>
                          </div>
                        </div>
                      </div>
                      <div class="path-group" v-if="minioProjectVersions.length > 0">
                        <div class="path-group-label">{{ projectName }}专属</div>
                        <div
                          class="path-item"
                          :class="{ selected: form.minioComponentId === v.id }"
                          v-for="v in minioProjectVersions"
                          :key="'cfg-' + v.id"
                          @click="selectMinio(v, 'configured')"
                        >
                          <HardDrive style="width: 14px; height: 14px;" class="file-icon" />
                          <div class="flex flex-col" style="gap: 2px;">
                            <span class="text-xs" style="color: var(--color-text-primary);">{{ v.name }}</span>
                            <span class="text-xs" style="color: var(--color-text-tertiary);">
                              含{{ projectName }}Bucket策略 · {{ projectName }} · {{ v.configuredSize }} ·
                              <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #2563EB; margin-right: 4px;"></span>已初始化
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-if="form.minioConfigState === 'clean'" class="flex items-center gap-4 mt-3 flex-wrap" style="padding: 8px 12px; background: var(--color-bg-sunken); border-radius: var(--radius-md);">
                  <div class="flex items-center gap-2">
                    <label class="text-xs" style="color: var(--color-text-tertiary);">API端口:</label>
                    <input type="text" v-model.number="form.minioApiPort" class="text-xs px-2.5 py-1.5 rounded-md" style="border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-primary); outline: none; width: 70px; font-family: var(--font-mono);">
                  </div>
                  <div class="flex items-center gap-2">
                    <label class="text-xs" style="color: var(--color-text-tertiary);">Console端口:</label>
                    <input type="text" v-model.number="form.minioConsolePort" class="text-xs px-2.5 py-1.5 rounded-md" style="border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-primary); outline: none; width: 70px; font-family: var(--font-mono);">
                  </div>
                  <div style="width: 1px; height: 24px; background: var(--color-border-light);"></div>
                  <div class="flex items-center gap-2">
                    <label class="text-xs" style="color: var(--color-text-tertiary);">账号:</label>
                    <input type="text" v-model="form.minioAccessKey" class="text-xs px-2.5 py-1.5 rounded-md" style="border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-primary); outline: none; width: 90px;">
                  </div>
                  <div class="flex items-center gap-2">
                    <label class="text-xs" style="color: var(--color-text-tertiary);">密码:</label>
                    <input type="password" v-model="form.minioSecretKey" class="text-xs px-2.5 py-1.5 rounded-md" style="border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-primary); outline: none; width: 100px;">
                  </div>
                </div>
                <div v-else class="flex items-center gap-2 mt-3" style="padding: 8px 12px; background: var(--state-info-bg); border-radius: var(--radius-md);">
                  <div class="flex items-center gap-1.5 text-xs" style="color: var(--state-info);">
                    <Info style="width: 12px; height: 12px;" />
                    <span>使用包内已有配置（端口、账密），系统不再覆盖</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Nginx -->
            <div class="req-row">
              <div class="req-row-icon" style="background: var(--state-warning-bg);">
                <Globe style="width: 18px; height: 18px; color: var(--state-warning);" />
              </div>
              <div class="req-row-body">
                <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                  <span class="req-row-name">Nginx</span>
                  <div class="path-dropdown" style="min-width: 240px;" :class="{ 'is-open': nginxDropdownOpen }">
                    <div class="path-dropdown-trigger" @click="toggleNginxDropdown">
                      <span>{{ nginxDisplayLabel }}</span>
                      <ChevronDown style="width: 14px; height: 14px; color: var(--color-text-tertiary); flex-shrink: 0;" />
                    </div>
                    <div class="path-dropdown-panel" v-show="nginxDropdownOpen">
                      <div class="path-group">
                        <div class="path-group-label">Nginx 版本</div>
                        <div
                          class="path-item"
                          :class="{ selected: form.nginxComponentId === v.id }"
                          v-for="v in nginxVersions"
                          :key="v.id"
                          @click="selectNginx(v)"
                        >
                          <Box style="width: 14px; height: 14px;" class="file-icon" />
                          <div class="flex flex-col" style="gap: 2px;">
                            <span class="text-xs" style="color: var(--color-text-primary);">{{ v.name }}</span>
                            <span class="text-xs" style="color: var(--color-text-tertiary);">
                              {{ v.label }} · 通用 · {{ v.size }} ·
                              <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #16A34A; margin-right: 4px;"></span>纯净版
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="flex items-center gap-3 mt-3" style="padding: 8px 12px; background: var(--color-bg-sunken); border-radius: var(--radius-md);">
                  <button class="flex items-center gap-2 px-3 py-1.5 rounded-md text-xs font-medium cursor-pointer border-0 transition-colors" style="background: var(--color-bg-elevated); border: 1px solid var(--color-border); color: var(--color-text-primary);"
                          @click="openNginxEditor">
                    <FileCode style="width: 13px; height: 13px; color: var(--color-primary);" />
                    编辑 nginx.conf
                  </button>
                  <p class="text-xs flex items-center gap-1.5" style="color: var(--color-text-tertiary);">
                    <Info style="width: 12px; height: 12px;" />
                    端口和路由规则在配置文件中定义，打包时自动覆盖
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Section C: 可选组件 -->
        <div class="rounded-lg p-6 mb-6" style="background: var(--color-bg-elevated); border: 1px solid var(--color-border-light); box-shadow: var(--shadow-sm);">
          <div class="flex items-center gap-3 mb-2">
            <div class="flex items-center justify-center rounded-md" style="width: 36px; height: 36px; background: var(--state-info-bg);">
              <Puzzle style="width: 18px; height: 18px; color: var(--state-info);" />
            </div>
            <div>
              <h2 class="font-heading text-base" style="color: var(--color-text-primary);">可选组件</h2>
              <p class="text-xs" style="color: var(--color-text-tertiary);">根据项目需求选择是否包含</p>
            </div>
          </div>

          <div style="margin-top: 16px;">
            <div class="opt-row">
              <input type="checkbox" v-model="form.includeEngine" class="opt-checkbox" />
              <div class="flex items-center gap-3 flex-1 flex-wrap">
                <div class="flex items-center gap-2">
                  <Cog style="width: 16px; height: 16px; color: var(--color-text-tertiary);" />
                  <span class="text-sm font-medium" style="color: var(--color-text-primary);">UnSim引擎</span>
                  <div class="path-dropdown" style="min-width: 240px;" :class="{ 'is-open': engineDropdownOpen }">
                    <div class="path-dropdown-trigger" @click="toggleEngineDropdown">
                      <span>{{ engineDisplayLabel }}</span>
                      <ChevronDown style="width: 14px; height: 14px; color: var(--color-text-tertiary); flex-shrink: 0;" />
                    </div>
                    <div class="path-dropdown-panel" v-show="engineDropdownOpen">
                      <div class="path-group">
                        <div class="path-group-label">UnSim引擎 版本</div>
                        <div
                          class="path-item"
                          :class="{ selected: form.engineComponentId === v.id }"
                          v-for="v in engineVersions"
                          :key="v.id"
                          @click="selectEngine(v)"
                        >
                          <Box style="width: 14px; height: 14px;" class="file-icon" />
                          {{ v.name }}
                          <span class="file-size">{{ v.size }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <span class="port-field" style="margin-left: auto;">
                  端口:
                  <input type="text" v-model.number="form.enginePort" class="port-input" />
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Section D: 脚本结构预览 -->
        <div class="rounded-lg p-6 mb-8" style="background: #1E293B; border: 1px solid #334155;">
          <div class="flex items-center gap-3 mb-5">
            <div class="flex items-center justify-center rounded-md" style="width: 36px; height: 36px; background: rgba(59,130,246,0.15);">
              <Terminal style="width: 18px; height: 18px; color: #60A5FA;" />
            </div>
            <div>
              <h2 class="font-heading text-base" style="color: #F1F5F9;">脚本结构预览</h2>
            </div>
          </div>

          <div class="rounded-lg p-5 mb-4" style="background: #0F172A; overflow-x: auto;">
<pre class="font-mono text-sm" style="color: #CBD5E1; line-height: 1.8; margin: 0; white-space: pre;">/
├── start.ps1               (总启动 - 按勾选启动服务)
├── stop.ps1                (总停止 - 根据PID杀死服务)
├── services/
│   ├── start-jar.ps1       (→ pid/jar.pid)
│   ├── start-mysql.ps1     (→ pid/mysql.pid)
│   ├── start-minio.ps1     (→ pid/minio.pid)
│   ├── start-nginx.ps1
│   ├── start-engine.ps1    (→ pid/engine.pid)
│   └── stop-*.ps1
└── pid/
    jar.pid, mysql.pid, minio.pid, engine.pid</pre>
          </div>

          <p class="text-xs flex items-center gap-1.5" style="color: #64748B;">
            <Info style="width: 12px; height: 12px;" />
            每个服务启动后记录PID，停止时根据PID精确终止进程
          </p>
        </div>

        <!-- Save Button -->
        <div class="flex items-center gap-3">
          <button class="flex items-center gap-2 px-6 py-2.5 rounded-lg text-sm font-medium cursor-pointer border-0 transition-colors" style="background: var(--color-primary); color: var(--color-text-inverse);"
                  @click="handleSave"
                  @mouseenter="saveBtnHover = true" @mouseleave="saveBtnHover = false"
                  :style="{ background: saveBtnHover ? 'var(--color-primary-hover)' : 'var(--color-primary)' }">
            <Save style="width: 16px; height: 16px;" />
            保存配置
          </button>
        </div>

      </div>
    </div>

    <!-- Nginx.conf Editor Modal -->
    <div v-show="nginxModalVisible" class="nginx-modal-overlay" @click.self="closeNginxEditor">
      <div class="nginx-modal">
        <div class="nginx-modal-header">
          <h3>编辑 nginx.conf</h3>
          <button class="nginx-modal-close" @click="closeNginxEditor">
            <X style="width: 18px; height: 18px;" />
          </button>
        </div>
        <div class="nginx-modal-body">
          <textarea v-model="nginxConfDraft"></textarea>
        </div>
        <div class="nginx-modal-footer">
          <button class="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium cursor-pointer border-0 transition-colors" style="background: var(--color-bg-sunken); border: 1px solid var(--color-border); color: var(--color-text-primary);"
                  @click="closeNginxEditor">
            取消
          </button>
          <button class="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium cursor-pointer border-0 transition-colors" style="background: var(--color-primary); color: var(--color-text-inverse);"
                  @click="saveNginxConf">
            <Save style="width: 14px; height: 14px;" />
            保存
          </button>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Save, Package, FolderOpen, Folder, FileArchive, ExternalLink,
  Blocks, Coffee, Database, HardDrive, Globe, Cog, Puzzle,
  Box, Info, FileCode, Terminal, X, ChevronDown
} from 'lucide-vue-next'
import AppSidebar from '@/components/AppSidebar.vue'
import AppHeader from '@/components/AppHeader.vue'
import { getProject, updateProject, getProjectConfig, saveProjectConfig } from '@/api/project'
import { listFiles } from '@/api/file'
import { getInfraList } from '@/api/infrastructure'
import { formatFileSize, formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => route.params.id)
const projectName = ref('项目')
const saveBtnHover = ref(false)

const defaultNginxConf = `worker_processes  1;

events {
    worker_connections  1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile        on;
    keepalive_timeout  65;

    server {
        listen       80;
        server_name  localhost;

        location / {
            root   html;
            index  index.html index.htm;
            try_files $uri $uri/ /index.html;
        }

        location /api/ {
            proxy_pass   http://127.0.0.1:8080/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }

        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   html;
        }
    }
}
`

const form = reactive({
  name: '',
  description: '',
  jarFileId: null,
  appPort: 8080,
  jvmParams: '-Xms512m -Xmx2048m',
  mysqlPort: 3306,
  mysqlVersion: '',
  mysqlConfigState: 'clean',
  dbName: '',
  dbUsername: 'root',
  dbPassword: '',
  minioApiPort: 9000,
  minioConsolePort: 9001,
  minioVersion: '',
  minioConfigState: 'clean',
  minioAccessKey: 'minioadmin',
  minioSecretKey: 'minioadmin',
  nginxVersion: '',
  nginxHttpPort: 80,
  nginxConfContent: defaultNginxConf,
  jdkVersion: '',
  includeEngine: false,
  engineVersion: '',
  enginePort: 8090,
  includeMysql: false,
  includeMinio: false,
  includeJdk: false,
  includeNginx: false,
  vueFolderId: null,
  // 组件文件ID（指向具体的 UploadedFile 记录）
  jdkComponentId: null,
  mysqlComponentId: null,
  minioComponentId: null,
  nginxComponentId: null,
  engineComponentId: null
})

const jdkVersions = ref([])
const mysqlVersions = ref([])
const minioVersions = ref([])
const nginxVersions = ref([])
const engineVersions = ref([])

// 按 belongsTo 分组：通用版本（belongsTo 为空或"通用"）和项目专属（belongsTo 匹配当前项目名）
const mysqlCommonVersions = computed(() =>
  mysqlVersions.value.filter(v => !v.belongsTo || v.belongsTo === '通用')
)
const mysqlProjectVersions = computed(() =>
  mysqlVersions.value.filter(v => v.belongsTo && v.belongsTo !== '通用' && v.belongsTo === projectName.value)
)
const minioCommonVersions = computed(() =>
  minioVersions.value.filter(v => !v.belongsTo || v.belongsTo === '通用')
)
const minioProjectVersions = computed(() =>
  minioVersions.value.filter(v => v.belongsTo && v.belongsTo !== '通用' && v.belongsTo === projectName.value)
)

async function loadInfraVersions() {
  const types = ['jdk', 'mysql', 'minio', 'nginx', 'engine']
  const targets = [jdkVersions, mysqlVersions, minioVersions, nginxVersions, engineVersions]
  for (let i = 0; i < types.length; i++) {
    try {
      const res = await getInfraList(types[i])
      const list = res?.records || res?.data || res || []
      // 保留文件ID，按ID唯一化（同一文件不去重），展示所有上传记录
      const seen = new Set()
      targets[i].value = list
        .filter(f => {
          if (seen.has(f.id)) return false
          seen.add(f.id)
          return true
        })
        .map(f => {
          const name = f.version || f.fileName
          return {
            id: f.id,
            key: f.id,
            name: name,
            label: f.tag || '',
            size: formatFileSize(f.fileSize),
            configuredSize: formatFileSize(f.fileSize),
            belongsTo: f.belongsTo || '',
            initState: f.initState || 'clean',
            fileName: f.fileName,
            fileSize: f.fileSize,
            version: f.version
          }
        })
    } catch (e) {
      console.error(e)
      targets[i].value = []
    }
  }
}

const jarOptions = ref([])
const vueOptions = ref([])

const jarDropdownOpen = ref(false)
const vueDropdownOpen = ref(false)
const jdkDropdownOpen = ref(false)
const mysqlDropdownOpen = ref(false)
const minioDropdownOpen = ref(false)
const nginxDropdownOpen = ref(false)
const engineDropdownOpen = ref(false)

const nginxModalVisible = ref(false)
const nginxConfDraft = ref('')

const selectedJarFile = computed(() => jarOptions.value.find(f => f.id === form.jarFileId))
const selectedVueFile = computed(() => vueOptions.value.find(f => f.id === form.vueFolderId))

const jdkDisplayLabel = computed(() => {
  const v = jdkVersions.value.find(x => x.id === form.jdkComponentId)
  return v ? `${v.name} — ${v.label} (${v.size})` : '请选择'
})

const mysqlDisplayLabel = computed(() => {
  const v = mysqlVersions.value.find(x => x.id === form.mysqlComponentId)
  if (!v) return '请选择'
  return v.initState === 'clean'
    ? `${v.name} — ${v.label} (${v.size})`
    : `${v.name} — 已初始化 (${v.configuredSize})`
})

const minioDisplayLabel = computed(() => {
  const v = minioVersions.value.find(x => x.id === form.minioComponentId)
  if (!v) return '请选择'
  return v.initState === 'clean'
    ? `${v.name} — ${v.label} (${v.size})`
    : `${v.name} — 已初始化 (${v.configuredSize})`
})

const nginxDisplayLabel = computed(() => {
  const v = nginxVersions.value.find(x => x.id === form.nginxComponentId)
  return v ? `${v.name} — ${v.label} (${v.size})` : '请选择'
})

const engineDisplayLabel = computed(() => {
  const v = engineVersions.value.find(x => x.id === form.engineComponentId)
  return v ? `${v.name} (${v.size})` : '请选择'
})

const jarPathLabel = computed(() => {
  if (!selectedJarFile.value) return '未选择'
  return `${projectName.value} / JAR包 / ${formatDate(selectedJarFile.value.createdAt)}`
})

const jarDisplayLabel = computed(() => {
  if (!selectedJarFile.value) return '请选择 JAR 文件'
  return `${selectedJarFile.value.fileName} (${formatSize(selectedJarFile.value.fileSize)})`
})

const vuePathLabel = computed(() => {
  if (!selectedVueFile.value) return '未选择'
  return `${projectName.value} / Vue产物 / ${formatDate(selectedVueFile.value.createdAt)}`
})

const vueDisplayLabel = computed(() => {
  if (!selectedVueFile.value) return '请选择 Vue 前端文件夹'
  return `${selectedVueFile.value.fileName} (${formatSize(selectedVueFile.value.fileSize)})`
})

const jarGrouped = computed(() => {
  const groups = {}
  jarOptions.value.forEach(f => {
    const date = formatDate(f.createdAt)
    const label = `${projectName.value} / JAR包 / ${date}`
    if (!groups[label]) groups[label] = []
    groups[label].push(f)
  })
  return Object.keys(groups).map(label => ({ label, items: groups[label] }))
})

const vueGrouped = computed(() => {
  const groups = {}
  vueOptions.value.forEach(f => {
    const date = formatDate(f.createdAt)
    const label = `${projectName.value} / Vue产物 / ${date}`
    if (!groups[label]) groups[label] = []
    groups[label].push(f)
  })
  return Object.keys(groups).map(label => ({ label, items: groups[label] }))
})

const breadcrumbList = computed(() => [
  { text: '项目管理', link: '/projects' },
  { text: projectName.value },
  { text: '项目配置' }
])

function formatSize(size) { return formatFileSize(size) }
function formatDT(t) { return formatDateTime(t) }

function formatDate(t) {
  if (!t) return ''
  const d = new Date(t)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function closeAllDropdowns() {
  jarDropdownOpen.value = false
  vueDropdownOpen.value = false
  jdkDropdownOpen.value = false
  mysqlDropdownOpen.value = false
  minioDropdownOpen.value = false
  nginxDropdownOpen.value = false
  engineDropdownOpen.value = false
}

function toggleJarDropdown(e) {
  e.stopPropagation()
  const v = !jarDropdownOpen.value
  closeAllDropdowns()
  jarDropdownOpen.value = v
}
function toggleVueDropdown(e) {
  e.stopPropagation()
  const v = !vueDropdownOpen.value
  closeAllDropdowns()
  vueDropdownOpen.value = v
}
function toggleJdkDropdown(e) {
  e.stopPropagation()
  const v = !jdkDropdownOpen.value
  closeAllDropdowns()
  jdkDropdownOpen.value = v
}
function toggleMysqlDropdown(e) {
  e.stopPropagation()
  const v = !mysqlDropdownOpen.value
  closeAllDropdowns()
  mysqlDropdownOpen.value = v
}
function toggleMinioDropdown(e) {
  e.stopPropagation()
  const v = !minioDropdownOpen.value
  closeAllDropdowns()
  minioDropdownOpen.value = v
}
function toggleNginxDropdown(e) {
  e.stopPropagation()
  const v = !nginxDropdownOpen.value
  closeAllDropdowns()
  nginxDropdownOpen.value = v
}
function toggleEngineDropdown(e) {
  e.stopPropagation()
  const v = !engineDropdownOpen.value
  closeAllDropdowns()
  engineDropdownOpen.value = v
}

function selectJar(f) {
  form.jarFileId = f.id
  jarDropdownOpen.value = false
}
function selectVue(f) {
  form.vueFolderId = f.id
  vueDropdownOpen.value = false
}
function selectJdk(v) {
  form.jdkComponentId = v.id
  form.jdkVersion = v.version || v.name
  form.includeJdk = true
  jdkDropdownOpen.value = false
}
function selectMysql(v, state) {
  form.mysqlComponentId = v.id
  form.mysqlVersion = v.version || v.name
  form.mysqlConfigState = v.initState === 'initialized' ? 'configured' : 'clean'
  form.includeMysql = true
  mysqlDropdownOpen.value = false
}
function selectMinio(v, state) {
  form.minioComponentId = v.id
  form.minioVersion = v.version || v.name
  form.minioConfigState = v.initState === 'initialized' ? 'configured' : 'clean'
  form.includeMinio = true
  minioDropdownOpen.value = false
}
function selectNginx(v) {
  form.nginxComponentId = v.id
  form.nginxVersion = v.version || v.name
  form.includeNginx = true
  nginxDropdownOpen.value = false
}
function selectEngine(v) {
  form.engineComponentId = v.id
  form.engineVersion = v.version || v.name
  form.includeEngine = true
  engineDropdownOpen.value = false
}

function handleDocClick() {
  closeAllDropdowns()
}

function goToUpload(type) {
  router.push({ path: '/upload', query: { projectId: projectId.value, type } })
}

function openNginxEditor() {
  nginxConfDraft.value = form.nginxConfContent
  nginxModalVisible.value = true
  document.body.style.overflow = 'hidden'
}

function closeNginxEditor() {
  nginxModalVisible.value = false
  document.body.style.overflow = ''
}

function saveNginxConf() {
  form.nginxConfContent = nginxConfDraft.value
  nginxModalVisible.value = false
  document.body.style.overflow = ''
  ElMessage.success('nginx.conf 已保存')
}

async function loadProject() {
  try {
    const res = await getProject(projectId.value)
    form.name = res.name || ''
    form.description = res.description || ''
    projectName.value = res.name || '项目'
    if (res.jarFileId) form.jarFileId = res.jarFileId
    if (res.vueFolderId) form.vueFolderId = res.vueFolderId
  } catch (e) {
    console.error(e)
  }
}

async function loadConfig() {
  try {
    const res = await getProjectConfig(projectId.value)
    if (res) {
      form.jarFileId = res.jarFileId || null
      form.vueFolderId = res.vueFolderId || null
      form.appPort = res.appPort || 8080
      form.jvmParams = res.jvmParams || '-Xms512m -Xmx2048m'
      form.mysqlPort = res.mysqlPort || 3306
      form.dbName = res.dbName || ''
      form.dbUsername = res.dbUsername || 'root'
      form.dbPassword = res.dbPassword || ''
      form.minioApiPort = res.minioApiPort || 9000
      form.minioConsolePort = res.minioConsolePort || 9001
      form.minioAccessKey = res.minioAccessKey || 'minioadmin'
      form.minioSecretKey = res.minioSecretKey || 'minioadmin'
      form.nginxHttpPort = res.nginxHttpPort || 80
      form.nginxConfContent = res.nginxConfContent || defaultNginxConf
      form.enginePort = res.enginePort || 8090
      // 组件ID（核心：指向具体的 UploadedFile 记录）
      form.jdkComponentId = res.jdkComponentId || null
      form.mysqlComponentId = res.mysqlComponentId || null
      form.minioComponentId = res.minioComponentId || null
      form.nginxComponentId = res.nginxComponentId || null
      form.engineComponentId = res.engineComponentId || null
      // 版本字符串和状态（用于显示和兼容）
      form.jdkVersion = res.jdkVersion || ''
      form.mysqlVersion = res.mysqlVersion || ''
      form.mysqlConfigState = res.mysqlConfigState || 'clean'
      form.minioVersion = res.minioVersion || ''
      form.minioConfigState = res.minioConfigState || 'clean'
      form.nginxVersion = res.nginxVersion || ''
      form.engineVersion = res.engineVersion || ''
      // include 标志：有组件ID即为true
      form.includeJdk = !!form.jdkComponentId || res.includeJdk === true
      form.includeMysql = !!form.mysqlComponentId || res.includeMysql === true
      form.includeMinio = !!form.minioComponentId || res.includeMinio === true
      form.includeNginx = !!form.nginxComponentId || res.includeNginx === true
      form.includeEngine = !!form.engineComponentId || res.engineEnabled === true
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadOptions() {
  try {
    const [jarRes, vueRes] = await Promise.all([
      listFiles({ projectId: projectId.value, type: 'jar' }),
      listFiles({ projectId: projectId.value, type: 'vue' })
    ])
    jarOptions.value = jarRes || []
    vueOptions.value = vueRes || []
  } catch (e) {
    console.error(e)
  }
}

async function handleSave() {
  try {
    await updateProject(projectId.value, {
      name: form.name,
      description: form.description,
      jarFileId: form.jarFileId,
      vueFolderId: form.vueFolderId
    })
    await saveProjectConfig(projectId.value, {
      jarFileId: form.jarFileId,
      vueFolderId: form.vueFolderId,
      appPort: form.appPort,
      jvmParams: form.jvmParams,
      mysqlPort: form.mysqlPort,
      dbName: form.dbName,
      dbUsername: form.dbUsername,
      dbPassword: form.dbPassword,
      minioApiPort: form.minioApiPort,
      minioConsolePort: form.minioConsolePort,
      minioAccessKey: form.minioAccessKey,
      minioSecretKey: form.minioSecretKey,
      nginxHttpPort: form.nginxHttpPort,
      nginxConfContent: form.nginxConfContent,
      engineEnabled: form.includeEngine,
      enginePort: form.enginePort,
      includeMysql: form.includeMysql,
      includeMinio: form.includeMinio,
      includeJdk: form.includeJdk,
      includeNginx: form.includeNginx,
      // 组件文件ID（核心字段）
      jdkComponentId: form.jdkComponentId,
      mysqlComponentId: form.mysqlComponentId,
      minioComponentId: form.minioComponentId,
      nginxComponentId: form.nginxComponentId,
      engineComponentId: form.engineComponentId,
      // 版本字符串和状态（兼容字段）
      jdkVersion: form.jdkVersion,
      mysqlVersion: form.mysqlVersion,
      mysqlConfigState: form.mysqlConfigState,
      minioVersion: form.minioVersion,
      minioConfigState: form.minioConfigState,
      nginxVersion: form.nginxVersion,
      engineVersion: form.engineVersion
    })
    ElMessage.success('保存成功')
  } catch (e) {
    console.error(e)
  }
}

onMounted(async () => {
  document.addEventListener('click', handleDocClick)
  await loadProject()
  await loadInfraVersions()
  await loadConfig()
  loadOptions()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocClick)
  document.body.style.overflow = ''
})
</script>

<style scoped>
.path-dropdown {
  position: relative;
}
.path-dropdown-trigger {
  width: 100%;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  font-size: 14px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  color: var(--color-text-primary);
  cursor: pointer;
  text-align: left;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: border-color 150ms;
}
.path-dropdown-trigger:hover {
  border-color: var(--color-primary);
}
.path-dropdown.is-open .path-dropdown-trigger {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(59,130,246,0.15);
}
.path-dropdown-panel {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  z-index: 50;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-float);
  max-height: 320px;
  overflow-y: auto;
  padding: 6px;
}
.path-group {
  padding: 4px 0;
}
.path-group + .path-group {
  border-top: 1px solid var(--color-border-light);
  margin-top: 4px;
  padding-top: 8px;
}
.path-group-label {
  font-size: 11px;
  color: var(--color-text-tertiary);
  padding: 2px 10px 6px;
  font-family: var(--font-mono);
}
.path-item {
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 13px;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background 100ms;
}
.path-item:hover {
  background: var(--color-bg-sunken);
}
.path-item.selected {
  background: var(--color-primary-light);
  color: var(--color-primary-text);
  font-weight: 500;
}
.path-item .file-icon {
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}
.path-item .file-size {
  font-size: 11px;
  color: var(--color-text-tertiary);
  margin-left: auto;
  flex-shrink: 0;
}

.req-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--color-border-light);
}
.req-row:last-child {
  border-bottom: none;
}
.req-row-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.req-row-body {
  flex: 1;
  min-width: 0;
}
.req-row-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.opt-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
}
.opt-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: var(--color-primary);
  flex-shrink: 0;
}
.port-field {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.port-input {
  width: 56px;
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: var(--color-bg-elevated);
  color: var(--color-text-primary);
  font-size: 12px;
  outline: none;
  text-align: center;
  font-family: var(--font-mono);
  transition: border-color 150ms;
}
.port-input:focus {
  border-color: var(--color-primary);
}

.nginx-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 1000;
  align-items: center;
  justify-content: center;
  display: flex;
}
.nginx-modal {
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  box-shadow: 0 24px 48px rgba(0,0,0,0.2);
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.nginx-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-light);
}
.nginx-modal-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}
.nginx-modal-close {
  width: 32px;
  height: 32px;
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
.nginx-modal-close:hover {
  background: var(--color-bg-sunken);
  color: var(--color-text-primary);
}
.nginx-modal-body {
  padding: 20px;
  flex: 1;
  overflow: hidden;
}
.nginx-modal-body textarea {
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.6;
  background: #1E293B;
  color: #E2E8F0;
  border: 1px solid #334155;
  border-radius: var(--radius-md);
  padding: 16px;
  width: 100%;
  min-height: 400px;
  resize: vertical;
  outline: none;
  box-sizing: border-box;
}
.nginx-modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid var(--color-border-light);
}
</style>
