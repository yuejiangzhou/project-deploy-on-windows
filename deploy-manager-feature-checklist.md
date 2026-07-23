# DeployManager 实现功能清单

> 基于 `deploy-manager/deploy-manager-requirements.md` 需文档和当前代码实现整理。状态说明：`[x]` 已完成 / `[/]` 部分实现（有缺陷） / `[ ]` 待开发。

**统计概览**：已完成 117 项 / 部分实现 0 项 / 待开发 0 项

---

## 模块 0：基础设施

### 0.1 后端框架
- [x] Spring Boot 项目初始化（JDK 8 兼容，Spring Boot 2.7.18）
- [x] MyBatis-Plus 配置（3.5.3.1，Slf4jImpl 日志）
- [x] MinIO 客户端配置（8.5.9，三个 bucket）
- [x] WebConfig / CORS 配置
- [x] logback-spring.xml（彩色控制台 + 滚动文件 + 错误独立归档）
- [x] 统一响应 Result / PageResult
- [x] FreeMarker 模板引擎配置

### 0.2 数据库
- [x] schema.sql 建表（project / uploaded_file / package_record / package_task）
- [x] uploaded_file 新增字段：version / tag / init_state / belongs_to
- [x] project 表新增字段：nginx_conf_content / jdk_version / mysql_version / mysql_config_state / minio_version / minio_config_state / nginx_version / engine_version / db_name / db_username / db_password / minio_access_key / minio_secret_key
- [x] migration.sql 增量迁移脚本
- [x] project 表新增非数据库字段：jarFileName / vueConfigured（@TableField(exist=false)）

### 0.3 依赖服务
- [x] docker-compose.yml（MySQL 8.0.27 + MinIO，pull_policy: never）
- [x] MinIO bucket 初始化（project-files / infra-components / packages）
- [x] application.yml 配置（端口 9090，500MB 上传限制，日志级别）

---

## 模块 1：项目管理页（ProjectList.vue）

### 1.1 布局
- [x] 侧边栏（220px 深色，2 导航项，底部状态 v2.5.0）
- [x] 顶部栏（面包屑 + 铃铛通知 + 用户头像）
- [x] 标题行（h1 + 批量下载按钮 + 新建项目按钮）
- [x] 统计卡片行（2 卡片：总项目数蓝、已打包数绿）
- [x] 搜索栏（单一输入框，max-width 320px）
- [x] 项目表格 + 分页器

### 1.2 功能
- [x] **F1.1 新建项目 Modal**（项目名称必填 + 描述选填 + 取消/创建）
- [x] **F1.2 批量下载**（弹出 Modal 选择项目，iframe 逐个下载最新包，支持全选有包项目）
- [x] **F1.3 项目列表展示**（表格：项目名称/JAR包/Vue前端/状态/操作）
- [x] **F1.4 动态项目图标**（按项目类型匹配 Lucide 图标 + 背景色：电商=ShoppingCart、数据=Database、用户=Users、审批=FileCheck、默认=Folder）
- [x] **F1.5 统计卡片**（总项目数 / 已打包数仅统计 SUCCESS）
- [x] **F1.6 实时搜索过滤**
- [x] **F1.7 行操作按钮**（配置蓝/打包绿/下载最新版本边框/删除灰）
- [x] **F1.8 删除二次确认**（ElMessageBox.confirm）
- [x] **F1.9 分页**（显示总页数+总记录数，上一页/下一页禁用逻辑完善）
- [x] **F1.10 下载最新版本提示弹窗**（先查询 getLatestPackage，无包时弹出 ElMessageBox 含"前往配置"和"我知道了"按钮）

### 1.3 状态徽章
- [x] Vue 前端：已配置(绿) / 未配置(灰)
- [x] 项目状态：已就绪(绿) / 配置中(蓝) / 已归档(灰)
- [x] JAR 包：文件名(等宽) / 未上传(灰)

### 1.4 后端接口
- [x] GET `/api/projects`（分页 + keyword 搜索 + 填充 jarFileName/vueConfigured）
- [x] POST `/api/projects`（创建，含默认配置）
- [x] PUT `/api/projects/{id}`（更新名称/描述）
- [x] DELETE `/api/projects/{id}`（逻辑删除）
- [x] GET `/api/projects/stats`（totalProjects / readyProjects / configuringProjects / archivedProjects / totalPackages 仅统计 SUCCESS）
- [x] GET `/api/projects/{id}/latest-package`（获取最新成功打包记录）
- [x] GET `/api/projects/{id}/latest-package/download`（下载最新打包产物）

---

## 模块 2：项目配置页（ProjectConfig.vue）

### 2.1 布局
- [x] 面包屑（项目管理 > {项目名} > 项目配置）
- [x] 顶部栏「前往打包中心」按钮
- [x] 页面标题「{项目名} - 项目配置」+ 副标题
- [x] Section A 项目文件卡片
- [x] Section B 基础组件卡片
- [x] Section C 可选组件卡片
- [x] Section D 脚本结构预览（深色卡片 #1E293B）
- [x] 保存配置按钮

### 2.2 Section A：项目文件
- [x] **路径下拉选择器**（触发器两行显示：灰色等宽路径 + 文件名；分组面板；选中蓝色高亮）
- [x] JAR 包路径选择 + 「管理文件」链接（跳转 /upload?projectId=&type=jar）
- [x] 应用端口输入（默认 8080，内联灰色框）
- [x] JVM 参数输入（默认 -Xms512m -Xmx2048m，等宽字体）
- [x] Vue 前端产物路径选择 + 「管理文件」链接

### 2.3 Section B：基础组件（版本选择）
- [x] **JDK 版本下拉**（动态加载 getInfraList('jdk')，显示版本名/标签/大小/纯净版标记）
- [x] **MySQL 版本下拉**（双分组：通用版本 clean + 项目专属 configured）
- [x] MySQL 纯净版配置区（端口/数据库名/账号/密码可编辑）
- [x] MySQL 已初始化提示（蓝色「使用包内已有配置」）
- [x] **配置状态联动**（selectMysql 同时记录 version 和 configState，切换显示不同输入区）
- [x] **MinIO 版本下拉**（双分组：通用 + 专属）
- [x] MinIO 纯净版配置区（API端口/Console端口/账号/密码）
- [x] MinIO 已初始化提示
- [x] **Nginx 版本下拉**（动态加载）
- [x] **Nginx.conf 编辑器 Modal**（800px 深色编辑区 #1E293B + 等宽字体 + 取消/保存 + 默认配置模板）
- [x] **mysqlConfigState/minioConfigState 回填**（loadConfig 已回填 jdkVersion/mysqlVersion/mysqlConfigState/minioVersion/minioConfigState/nginxVersion/engineVersion，handleSave 也发送 mysqlConfigState/minioConfigState）

### 2.4 Section C：可选组件
- [x] UnSim 引擎复选框（form.includeEngine）
- [x] 引擎版本下拉（动态加载 getInfraList('engine')）
- [x] 引擎端口输入（默认 8090）

### 2.5 Section D：脚本预览
- [x] 目录树预览（等宽字体，start.ps1/stop.ps1/services/pid 结构）
- [x] PID 管理说明文字「每个服务启动后记录PID，停止时根据PID精确终止进程」

### 2.6 后端接口
- [x] GET `/api/projects/{id}/config`（返回 ProjectConfigDTO）
- [x] POST `/api/projects/{id}/config`（保存配置，含版本和 configState）
- [x] GET `/api/files/list?projectId=&type=`（路径下拉数据源，前端用 listFiles 加载）
- [x] GET `/api/infra/{type}/list`（基础组件版本列表，前端 loadInfraVersions 动态加载）

---

## 模块 3：资源上传页（ResourceUpload.vue）

### 3.1 布局
- [x] 页面标题 + 副标题
- [x] Category 1 标题卡片（蓝色 folder-open 图标）
- [x] Category 1 双栏布局（左 300px 树 + 右文件列表）
- [x] Category 2 标题卡片（绿色 blocks 图标）
- [x] Category 2 子标签栏（JDK/MySQL/MinIO/Nginx/UnSim引擎，active 白底蓝字）

### 3.2 Category 1：项目文件
- [x] **文件夹树组件**（两级：项目 > 类型，展开/折叠，文件数显示）
- [x] 树节点选中蓝色高亮 + chevron 旋转动画
- [x] 右栏面包屑（项目名 > 类型）
- [x] **JAR 上传区**（UploadCloud 图标 + accept=.jar + 版本标签输入 + 上传按钮）
- [x] **Vue 产物上传区**（FolderUp 图标 + webkitdirectory + 版本标签 + 自动识别 folderName）
- [x] 同名覆盖警告（黄色 AlertTriangle + 提示文字）
- [x] 文件表格（文件名/版本标签/大小/上传时间/操作删除）
- [x] 空状态（folder-open + 暂无文件）
- [x] 全屏 loading 提示（ElLoading.service，上传中遮罩）

### 3.3 Category 2：基础组件
- [x] 子标签切换（5 个 tab，infraFileMap 初始化所有 key）
- [x] 版本列表标题 + 版本数徽章（>0 蓝色 / =0 灰色）+ 上传新版本按钮
- [x] **JDK 版本表格**（版本/标签/文件名/大小/上传时间/操作详情删除）
- [x] **MySQL 版本表格**（版本/标签/初始化状态/文件名/大小/上传时间/所属系统/操作）
- [x] **MinIO 版本表格**（同 MySQL 列）
- [x] **Nginx 版本表格**（版本/标签/文件名/大小/上传时间/操作）
- [x] **引擎版本表格**（同 Nginx 列）
- [x] 初始化状态徽章（纯净版绿 / 已初始化蓝）
- [x] 所属系统徽章（通用灰 / 项目名蓝）
- [x] 空状态提示「暂无数据，点击右上角"上传新版本"添加」
- [x] **折叠式上传区**（点击上传新版本展开）
  - [x] JDK 上传区（FolderUp + 版本100px/标签100px + 底部说明「JDK 仅支持纯净版」）
  - [x] MySQL 上传区（FolderUp + 版本/标签/初始化状态/所属系统 + 底部说明）
  - [x] MinIO 上传区（UploadCloud + .zip + 版本/标签/初始化/所属系统 + 底部说明）
  - [x] Nginx 上传区（UploadCloud + .zip + 版本/标签 + 底部说明）
  - [x] 引擎上传区（FolderUp + .zip或文件夹 + 版本80px/标签140px + 底部说明）
- [x] **组件详情 Modal**（版本/标签/初始化状态/所属系统/文件名只读/大小只读/上传时间只读 + 取消/保存）
- [x] 全屏 loading 提示（所有基础组件上传操作）
- [x] **拖拽上传**（Category 1 和 Category 2 上传区均绑定 @dragover.prevent/@dragleave/@drop.prevent，支持文件拖拽）
- [x] **组件详情编辑保存**（详情 Modal version/tag/initState/belongsTo 可编辑，新增保存按钮调用 editInfraFile API）

### 3.4 后端接口
- [x] GET `/api/files/tree?projectId=`（两级树：项目 > 类型，含文件数）
- [x] POST `/api/files/upload`（JAR 单文件 + projectId + type + versionTag）
- [x] POST `/api/files/upload-folder`（Vue 文件夹 + projectId + type + folderName + versionTag）
- [x] GET `/api/files/list?projectId=&type=`
- [x] DELETE `/api/files/{id}`
- [x] GET `/api/infra/{type}/list`（含 version/tag/initState/belongsTo）
- [x] POST `/api/infra/{type}/upload`（单文件，含版本/标签/初始化状态/所属系统）
- [x] POST `/api/infra/{type}/upload-folder`（文件夹，含版本/标签/初始化状态/所属系统）
- [x] DELETE `/api/infra/{type}/{id}`
- [x] PUT `/api/infra/{type}/{id}`（编辑详情：版本/标签/初始化状态/所属系统）
- [x] ZIP 上传自动解压（大文件夹场景 — 后端 uploadComponentZip + Zip4j 解压 + 前端"或上传ZIP"按钮）

---

## 模块 4：打包中心页（PackageCenter.vue）

### 4.1 布局
- [x] 面包屑（项目管理 > {项目名} > 打包中心）
- [x] Section 1 当前配置卡片（只读）
- [x] Section 2 打包进度卡片
- [x] Section 3 打包产物列表卡片

### 4.2 Section 1：当前配置
- [x] 标题 + 副标题 + 「返回项目配置页」链接
- [x] 配置列表（8 行只读：JAR/Vue/JDK/MySQL/MinIO/引擎/Nginx/启动脚本）
- [x] 各行图标 + 名称 + 版本/文件名 + 大小 + 来源/端口 + 状态标签
- [x] 「账密加密」标签（MySQL/MinIO）
- [x] 「固定包含」标签（JDK）
- [x] **ZIP 加密设置**（密码 + 确认密码 + show-password eye 切换 + 蓝色提示）
- [x] **开始打包按钮**（校验密码非空+一致后启动）

### 4.3 Section 2：打包进度
- [x] 进度条（百分比 + 步骤文字 + 蓝色脉冲点 + 灰色底蓝色填充）
- [x] **终端日志区**（深色 #0F172A + 等宽 + 时间戳灰 + 内容白 + OK 绿 + 自动滚动）
- [x] **真实打包进度对接**（startPackage 先调用 startPackageApi 获取 taskId，然后 pollPackageProgress 每秒轮询 getPackageProgress 更新进度和日志）

### 4.4 Section 3：打包产物
- [x] 筛选按钮组（全部/成功/失败，active 蓝底）
- [x] 产物表格（文件名/大小/加密/时间/状态/操作）
- [x] 加密列（是绿锁 Lock / 否灰开锁 Unlock）
- [x] 状态列（成功绿徽章 / 失败红徽章）
- [x] 下载按钮（仅 SUCCESS 可下载，window.open）
- [x] 删除按钮（ElMessageBox 确认后 deletePackage）

### 4.5 后端接口
- [x] POST `/api/package/start`（projectId + password → taskId）
- [x] GET `/api/package/progress/{taskId}`（status/progress/currentStep/logs[]）
- [x] GET `/api/packages?projectId=&status=&pageNum=&pageSize=`
- [x] GET `/api/packages/{id}/download`
- [x] DELETE `/api/packages/{id}`

### 4.6 已知 Bug
- [x] **面包屑链接已修复**：PackageCenter 已将 `to:` 改为 `link:`，与 AppHeader 的 `crumb.link` 检查一致

---

## 模块 5：打包核心流程（后端 PackageService）

- [x] 打包配置完整性校验（JAR/组件/密码非空）
- [x] 从 MinIO 下载文件到临时目录
- [x] 收集 JAR → {packageDir}/app/
- [x] 收集 JDK → {packageDir}/jdk/
- [x] 收集 Vue 产物 + Nginx 配置 → {packageDir}/nginx/
- [x] 收集 MySQL → {packageDir}/mysql/
- [x] 收集 MinIO → {packageDir}/minio/
- [x] [可选] 收集引擎 → {packageDir}/engine/
- [x] 生成启动脚本（ScriptTemplateService + FreeMarker 模板渲染）
  - [x] start.ps1 / stop.ps1
  - [x] services/start-jar.ps1 / stop-jar.ps1
  - [x] services/start-mysql.ps1 / stop-mysql.ps1
  - [x] services/start-minio.ps1 / stop-minio.ps1
  - [x] services/start-nginx.ps1 / stop-nginx.ps1
  - [x] services/start-engine.ps1 / stop-engine.ps1（可选）
  - [x] pid/ 目录创建
- [x] Nginx nginx.conf 覆盖（collectNginxConf）
- [x] ZIP 加密打包（Zip4j AES-256，createEncryptedZip）
- [x] 上传 ZIP 到 MinIO（uploadZipToMinio）
- [x] 记录 package_record（savePackageRecord）
- [x] 异步任务（@Async executePackageAsync）
- [x] 进度日志推送（updateTaskProgress / addLog，日志上限 500 条）
- [x] 临时目录清理（deleteDirectory）
- [x] **纯净版注入账密**（MySQL: my.ini 生成 + mysqld --initialize-insecure + init.sql 创建数据库和账号；MinIO: MINIO_ROOT_USER/MINIO_ROOT_PASSWORD 环境变量注入）
- [x] **已初始化版跳过注入**（FreeMarker <#if configState == "clean"> 条件分支，已初始化版直接启动不注入）
- [x] **按需收集组件**（PackageService 按 includeMysql/includeMinio/includeNginx 条件收集，generateAppConfig 按需输出 datasource/minio 配置）
- [x] **脚本模板条件渲染**（start.ps1.ftl/stop.ps1.ftl 的 Nginx/MySQL/MinIO 启动停止均按 enabled 标志条件渲染）

---

## 模块 6：公共组件

- [x] AppSidebar.vue（侧边栏，220px 深色，2 导航项，底部状态）
- [x] AppHeader.vue（顶部栏 + 面包屑 + 铃铛 + 用户头像）
- [x] 路径下拉选择器（内联在 ProjectConfig.vue，自定义 path-dropdown 组件）
- [x] 文件夹树节点（内联在 ResourceUpload.vue）
- [x] 版本下拉选择器（内联在 ProjectConfig.vue）
- [x] 上传区域组件（内联在 ResourceUpload.vue）
- [x] NginxConf 编辑器 Modal（内联在 ProjectConfig.vue）
- [x] 组件详情 Modal（内联在 ResourceUpload.vue）
- [x] 终端日志组件（内联在 PackageCenter.vue）
- [x] 确认弹窗 / Toast 通知（ElMessageBox / ElMessage）

---

## 模块 7：脚本模板（FreeMarker）

- [x] start.ps1.ftl（总启动，按启用服务顺序编号）
- [x] stop.ps1.ftl（总停止，反序）
- [x] services/start-jar.ps1.ftl（JVM 参数 + 端口 + PID）
- [x] services/stop-jar.ps1.ftl（按 PID 终止）
- [x] services/start-mysql.ps1.ftl（端口 + datadir + PID）
- [x] services/stop-mysql.ps1.ftl
- [x] services/start-minio.ps1.ftl（API/Console 端口 + PID）
- [x] services/stop-minio.ps1.ftl
- [x] services/start-nginx.ps1.ftl
- [x] services/stop-nginx.ps1.ftl
- [x] services/start-engine.ps1.ftl（端口 + PID）
- [x] services/stop-engine.ps1.ftl

---

## 模块 8：前端 API 层

- [x] project.js：getProjectList / getProjectStats / getProject / createProject / updateProject / deleteProject / getProjectConfig / saveProjectConfig / getLatestPackage / downloadLatestPackageUrl
- [x] file.js：getFileTree / listFiles / getPathOptions / uploadFile / uploadFolder / deleteFile / initChunkedUpload / uploadChunk / completeChunkedUpload / abortChunkedUpload / uploadLargeFile
- [x] infrastructure.js：getInfraList / uploadInfraFile / uploadInfraFolder / uploadInfraZip / deleteInfraFile / getLatestInfra
- [x] package.js：startPackage / getPackageProgress / getPackages / deletePackage / getDownloadUrl
- [x] operationLog.js：getOperationLogs（分页查询操作日志）
- [x] utils/request.js：Axios 封装（baseURL=/api，timeout=600000，响应拦截器）
- [x] utils/format.js：formatFileSize / formatDate / formatDateTime

---

## 模块 9：操作日志审计

### 9.1 后端
- [x] OperationLog 实体（id/module/action/targetType/targetId/targetName/detail/status/operator/ip/createdAt）
- [x] OperationLogMapper（BaseMapper）
- [x] OperationLogService（@Async 异步记录 + 分页查询）
- [x] OperationLogController（GET `/api/operation-logs` 分页查询）
- [x] operation_log 表 DDL（schema.sql + migration.sql）
- [x] ProjectController 接入日志（createProject/updateProject/deleteProject/saveConfig/downloadLatestPackage）
- [x] PackageController 接入日志（startPackage/downloadPackage/deletePackage）
- [x] FileController 接入日志（uploadFile/uploadFolder/deleteFile）
- [x] InfrastructureController 接入日志（所有上传/删除操作）

### 9.2 前端
- [x] operationLog.js API 封装

---

## 模块 10：大文件分片上传

### 10.1 后端
- [x] ChunkedUploadService（init/uploadChunk/completeUpload/abort 四步协议）
- [x] ChunkUploadSession 内部类（uploadId/fileName/fileSize/totalChunks/chunks Map/projectId/type/versionTag/startTime）
- [x] SequenceChunkInputStream 自定义流（按 chunkIndex 顺序合并分片）
- [x] @PostConstruct/@PreDestroy 生命周期管理（启动清理过期会话 / 停止清理所有临时文件）
- [x] FileController 分片端点（/upload/chunk-init / /upload/chunk / /upload/chunk-complete / /upload/chunk-abort/{uploadId}）

### 10.2 前端
- [x] file.js 分片上传函数（initChunkedUpload / uploadChunk / completeChunkedUpload / abortChunkedUpload / uploadLargeFile）
- [x] ResourceUpload.vue JAR 上传 >100MB 自动使用分片上传 + 进度显示

---

## 模块 11：打包任务并发控制

- [x] Semaphore 并发控制（@Value ${package.max-concurrent-tasks:3} + @PostConstruct initSemaphore）
- [x] ConcurrentHashMap<Long, String> activeProjectTasks 内存防重
- [x] DB 双重检查（启动时查询 PENDING/RUNNING 状态任务，防止重启后状态不一致）
- [x] startPackage 检查 + executePackageAsync acquire/release（finally 释放）
- [x] 排队等待日志提示（addLog "排队等待打包资源..."）

---

## 待修复问题汇总

### 已全部修复（2026-07-13）
1. ~~**[模块4] 打包进度前端模拟**~~：已对接后端真实进度接口 getPackageProgress，替换 setTimeout 模拟
2. ~~**[模块2] mysqlConfigState/minioConfigState 回填缺失**~~：loadConfig 已回填，handleSave 也已发送
3. ~~**[模块4] 面包屑链接不可点**~~：已将 `to:` 改为 `link:`，属性名一致
4. ~~**[模块1] 下载最新版本提示弹窗**~~：已实现 getLatestPackage 查询 + ElMessageBox 提示弹窗
5. ~~**[模块1] 批量下载**~~：已实现批量下载 Modal + iframe 逐个下载
6. ~~**[模块1] 分页完善**~~：已显示总页数、禁用下一页按钮
7. ~~**[模块3] 拖拽上传**~~：已为两个上传区绑定 @dragover/@drop 事件
8. ~~**[模块3] 组件详情编辑**~~：详情 Modal 可编辑 + 新增 PUT 接口
9. ~~**[模块3] ZIP 上传自动解压**~~：后端 uploadComponentZip + Zip4j 解压 + 前端"或上传ZIP"按钮
10. ~~**[模块5] 纯净版账密注入**~~：MySQL 生成 my.ini + mysqld --initialize-insecure + init.sql 创建数据库和账号；MinIO 通过 MINIO_ROOT_USER/MINIO_ROOT_PASSWORD 环境变量注入
11. ~~**[模块5] 已初始化版跳过注入**~~：FreeMarker `<#if configState == "clean">` 条件分支，已初始化版直接启动不注入
12. ~~**[模块3/5] 大文件分片上传**~~：ChunkedUploadService 四步协议（init/chunk/complete/abort）+ SequenceChunkInputStream 合并 + 前端 >100MB 自动分片
13. ~~**[模块5] 打包任务并发控制**~~：Semaphore + ConcurrentHashMap 防重 + DB 双重检查
14. ~~**[模块9] 操作日志审计**~~：OperationLog 实体 + @Async 异步记录 + 分页查询 + 全控制器接入
15. ~~**[模块5] @Async 自调用失效**~~：注入自身代理 `self.executePackageAsync()` + 自定义 ThreadPoolTaskExecutor 线程池
16. ~~**[模块5] 无自定义线程池**~~：AsyncConfig 配置类，核心线程数读取 `package.max-concurrent-tasks`
17. ~~**[模块5] configured 时覆盖账密**~~：generateAppConfig 仅 `configState=="clean"` 时写入账密
18. ~~**[模块5] ZIP 密码明文存储**~~：任务完成后清空 `PackageTask.password` 字段
19. ~~**[模块5] taskLogs 内存清理**~~：finally 块中 `taskLogs.remove(taskId)`
20. ~~**[模块5] ZIP 文件名不含 taskId**~~：文件名改为 `projectName-taskId-release.zip`
21. ~~**[模块5] 缺少项目状态校验**~~：startPackage 校验 `project.status == "READY"`
22. ~~**[模块5] JDK 缺失仅 warn**~~：JDK 缺失时抛异常终止打包
23. ~~**[模块5] 事务回滚不覆盖 MinIO**~~：uploadFolder/uploadComponentFolder 补偿清理已上传 MinIO 对象
24. ~~**[API] 全局异常处理器**~~：GlobalExceptionHandler 统一捕获异常返回 Result 格式
25. ~~**[API] pageNum=0 非法页码**~~：Controller 层 `Math.max(1, pageNum)`
26. ~~**[API] getProject 未填充 jarFileName**~~：getProjectById 补充非数据库字段
27. ~~**[API] Controller 无 @Valid 校验**~~：saveConfig 加 @Valid，PackageStartRequest.projectId 加 @NotNull
28. ~~**[API] 不存在的项目返回 200**~~：getProject 返回 `Result.error(404, "项目不存在")`
29. ~~**[API] type 参数无白名单**~~：FileController 增加 validateType 校验
30. ~~**[配置] migration.sql 非幂等**~~：改为存储过程 + INFORMATION_SCHEMA 判断
31. ~~**[配置] migration.sql 缺 project 表增量**~~：补充 project 表 12 个字段的 ALTER TABLE
32. ~~**[配置] HikariCP 连接池**~~：application.yml 添加连接池配置
33. ~~**[配置] CORS 过于宽松**~~：限定为 `localhost:*`、`127.0.0.1:*`
34. ~~**[前端] 分页器缺页码按钮**~~：增加页码按钮组 + 省略号
35. ~~**[前端] 详情 Modal 保存无校验**~~：`saveDetail` 校验 version 非空
36. ~~**[前端] 面包屑缺第 3 级**~~：改为 `项目管理 > 项目名 > 打包中心`
37. ~~**[前端] 轮询定时器未清理**~~：`onBeforeUnmount` 清除定时器
38. ~~**[前端] 搜索输入无防抖**~~：300ms debounce
39. ~~**[前端] loadProject/loadConfig 竞态**~~：改为串行 await
40. ~~**[前端] fileMap 缓存未清空**~~：上传/删除后清空缓存
41. ~~**[前端] Nginx 编辑器 overflow 未清理**~~：`onBeforeUnmount` 恢复 body overflow
42. ~~**[前端] 密码校验无复杂度**~~：最小长度 6 位
43. ~~**[前端] 轮询失败无感知**~~：连续失败 3 次停止并提示
44. ~~**[前端] 日志增量比对错位**~~：改为基于时间戳比对
45. ~~**[前端] 搜索无 loading 状态**~~：增加 loading 指示器
46. ~~**[前端] 前端构建 chunk 过大**~~：manualChunks 拆分 vue-vendor/element-plus/lucide

---

## 开发优先级建议

### P0 - 已全部修复
1. ~~打包进度对接后端真实接口~~
2. ~~mysqlConfigState/minioConfigState 回填~~
3. ~~面包屑链接属性修复~~
4. ~~@Async 自调用失效修复~~
5. ~~自定义线程池配置~~
6. ~~文件名路径穿越校验~~
7. ~~全局异常处理器~~
8. ~~schema.sql 与 migration.sql 字段冲突~~
9. ~~轮询定时器未清理~~

### P1 - 已全部完善
1. ~~下载最新版本提示弹窗~~
2. ~~分页完善~~
3. ~~拖拽上传支持~~
4. ~~组件详情编辑保存~~
5. ~~批量下载功能~~
6. ~~configured 时覆盖账密~~
7. ~~ZIP 密码明文存储~~
8. ~~taskLogs 内存清理~~
9. ~~ZIP 文件名含 taskId~~
10. ~~项目状态校验~~
11. ~~事务回滚不覆盖 MinIO~~
12. ~~pageNum=0 非法页码~~
13. ~~getProject 未填充 jarFileName~~
14. ~~Controller @Valid 校验~~
15. ~~migration.sql 幂等+增量字段~~
16. ~~HikariCP 连接池配置~~
17. ~~分页器缺页码按钮~~
18. ~~详情 Modal 保存校验~~
19. ~~面包屑缺第 3 级~~
20. ~~feature-checklist 误报~~

### P2 - 已全部完成
1. ~~ZIP 上传自动解压~~（后端 Zip4j 解压 + 前端"或上传ZIP"按钮）
2. ~~大文件分片上传~~（ChunkedUploadService + 前端 >100MB 自动分片 + 进度显示）
3. ~~打包任务并发控制~~（Semaphore + activeProjectTasks + DB 双重检查）
4. ~~操作日志审计~~（OperationLog + @Async 异步 + 分页查询 + 全控制器接入）
5. ~~纯净版/已初始化版账密注入~~（FreeMarker 条件渲染 + MySQL/MinIO 账密注入脚本）
6. ~~看全部 P2 优化~~（文件路径穿越、catch 堆栈丢失、substring 越界、URLEncoder 不一致、搜索防抖、构建 chunk 拆分等 16 项）

---

*所有功能已全部实现，所有 bug 已全部修复，所有文档已同步更新。清单结束。*
