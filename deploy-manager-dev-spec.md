# DeployManager 开发技术规范

> 本文档基于 `deploy-manager/deploy-manager-requirements.md` 需求文档和当前代码实现整理，是系统开发的权威技术规范。如需求文档与本文档冲突，以需求文档为准；如原型与需求文档冲突，以原型视觉效果为准。

---

## 1. 项目概述

### 1.1 产品定位

DeployManager 是面向公司内部开发人员的**多产品部署打包管理系统**。将各产品的 Java 后端（JAR）、Vue 前端产物（dist）、MySQL 数据库便携包、MinIO 对象存储、Nginx Web 服务器及可选的 UnSim 引擎，统一打包为**加密 ZIP**，交由运维/销售人员部署到客户 Windows 环境。

### 1.2 核心业务流程

```
开发上传资源(项目文件 + 基础组件) → 配置项目(选版本/端口/账密) → 打包生成加密ZIP → 运维下载ZIP → 客户Windows解压部署
```

### 1.3 技术栈

| 层级 | 技术选型 | 版本 |
|------|---------|------|
| 前端 | Vue 3 + Vite + Element Plus + Tailwind CSS + Lucide Icons | Vue 3.4 / Vite 5 / Element Plus 2.6 |
| 后端 | Java 8 + Spring Boot + Maven | JDK 8 / Spring Boot 2.7.18 |
| ORM | MyBatis-Plus | 3.5.3.1 |
| 数据库 | MySQL | 8.0.27（Docker 容器） |
| 文件存储 | MinIO | 8.5.9 客户端 |
| ZIP 加密 | Zip4j（AES-256） | 2.11.5 |
| 脚本模板 | FreeMarker | Spring Boot 集成 |
| 工具类 | Hutool / Lombok | 5.8.25 / 提供 |

### 1.4 设计原型

| 页面 | 原型文件 | 路由 |
|------|---------|------|
| 项目管理 | `deploy-manager/pages/project-list.html` | `/projects` |
| 项目配置 | `deploy-manager/pages/project-config.html` | `/projects/:id/config` |
| 资源上传 | `deploy-manager/pages/resource-upload.html` | `/upload` |
| 打包中心 | `deploy-manager/pages/package-center.html` | `/projects/:id/package` |

---

## 2. 硬性约束

> 以下为不可违反的开发约束，来源于项目历史决策和工程规范。

### 2.1 JDK 8 兼容性
- 后端代码必须兼容 JDK 8，禁止使用 JDK 9+ 方法
- 使用 `Files.write(path, str.getBytes(StandardCharsets.UTF_8))` 而非 `Files.writeString()`
- 使用 `URLEncoder.encode(name, "UTF-8")` 而非 `URLEncoder.encode(name, StandardCharsets.UTF_8)`
- `maven-compiler-plugin` 的 source/target 必须为 1.8

### 2.2 Docker 服务
- docker-compose.yml 中所有服务必须设置 `pull_policy: never`，强制使用本地镜像
- 禁止远程镜像拉取，避免启动时网络检查
- 服务包括：MySQL 8.0.27（localhost:3306）、MinIO（localhost:9000/9001）

### 2.3 日志规范
- 日志输出必须包含：时间戳、日志级别、PID、线程、logger 名称、消息
- 控制台日志必须彩色、UTF-8 编码
- 错误日志独立归档到 `logs/deploy-manager-error.log`
- 日志文件按天滚动或 200MB 滚动，保留 30 天，总大小上限 5GB
- dev/default profile 同时输出到控制台和文件；prod profile 仅输出到文件
- MyBatis SQL 日志必须使用 `Slf4jImpl`（不可用 `StdOutImpl`）
- 日志配置通过 `logback-spring.xml` 管理

### 2.4 前端规范
- 优先匹配原型实现的视觉效果，即使开发速度较慢
- 图标统一使用 `lucide-vue-next`（Element Plus 图标虽全局注册但实际不使用）
- 样式主体靠 Tailwind CSS + CSS 变量（`src/styles/variables.css`）
- 组件内部使用 ref/reactive 管理状态（Pinia 已注册但未使用 store）

### 2.5 代码管理
- 代码修改后必须同步更新「实现功能清单」确保代码与清单一致
- 保留历史 commit，避免增加同事的 merge 工作量
- 可基于特定 commit 进行后续开发

---

## 3. 前端设计规范

### 3.1 全局布局

- 左侧固定侧边栏（220px，深色背景 `#0F172A`）
- 右侧主内容区（顶部栏 56px + 滚动内容区）
- 打包中心、项目配置为钻入页，侧边栏无高亮

### 3.2 配色与字体（CSS 变量）

定义在 `src/styles/variables.css`：

```css
:root {
  --color-primary: #3B82F6;
  --color-primary-hover: #2563EB;
  --color-primary-light: #EFF6FF;
  --color-bg: #F8FAFC;
  --color-bg-elevated: #FFFFFF;
  --color-bg-sunken: #F1F5F9;
  --color-border: #CBD5E1;
  --color-border-light: #E2E8F0;
  --color-text-primary: #0F172A;
  --color-text-secondary: #475569;
  --color-text-tertiary: #94A3B8;
  --state-success: #16A34A;
  --state-warning: #D97706;
  --state-error: #DC2626;
  --radius-md: 8px;
  --radius-lg: 12px;
  --font-family: 'Inter', -apple-system, sans-serif;
  --font-mono: 'JetBrains Mono', monospace;
}
```

Tailwind 配置（`tailwind.config.js`）将 colors/borderRadius/fontFamily/boxShadow 映射到上述 CSS 变量。

### 3.3 侧边栏（AppSidebar.vue）

- 220px 宽，深色背景 `#0F172A`
- 顶部 logo：Rocket 图标 + "DeployManager"
- 2 个导航项：项目管理（FolderOpen）、资源上传（UploadCloud）
- 当前页高亮：蓝底白字（`#3B82F6` / `#FFFFFF`）
- 非高亮项：灰色文字（`#94A3B8`），hover 浅色背景
- 底部：绿色圆点 + "系统正常运行 v2.5.0"
- 钻入页（配置/打包）侧边栏不高亮任何项

### 3.4 顶部栏（AppHeader.vue）

- 左侧：面包屑导航（ChevronRight 分隔），仅 `crumb.link` 存在时渲染为可点击链接
- 右侧：Bell 通知图标（带红点）+ 蓝色圆形头像 + 用户名

### 3.5 前端项目结构

```
deploy-manager-web/
├── package.json
├── vite.config.js          (端口 5173，/api 代理到 localhost:9090)
├── tailwind.config.js      (映射 CSS 变量)
├── postcss.config.js
├── src/
│   ├── main.js             (注册 Pinia + Router + ElementPlus + 全局图标)
│   ├── App.vue             (极简根组件，仅 router-view)
│   ├── router/index.js     (4 条路由)
│   ├── api/
│   │   ├── project.js      (项目 CRUD + 配置 + 统计 + 最新包)
│   │   ├── file.js         (文件树 + 上传 + 删除)
│   │   ├── infrastructure.js (基础组件 CRUD)
│   │   └── package.js      (打包启动 + 进度 + 产物管理)
│   ├── views/
│   │   ├── ProjectList.vue       (项目管理页)
│   │   ├── ProjectConfig.vue     (项目配置页)
│   │   ├── ResourceUpload.vue    (资源上传页)
│   │   └── PackageCenter.vue     (打包中心页)
│   ├── components/
│   │   ├── AppSidebar.vue
│   │   └── AppHeader.vue
│   ├── styles/
│   │   ├── variables.css   (CSS 变量定义)
│   │   └── global.css      (Tailwind 引入 + reset + Element Plus 覆盖)
│   └── utils/
│       ├── request.js      (Axios 封装，baseURL=/api，timeout=600000)
│       └── format.js       (formatFileSize / formatDate / formatDateTime)
```

### 3.6 路由配置

| 路径 | name | 组件 | meta.sidebarActive |
|------|------|------|-------------------|
| `/` | - | redirect → `/projects` | - |
| `/projects` | ProjectList | ProjectList.vue | `projects` |
| `/projects/:id/config` | ProjectConfig | ProjectConfig.vue | `null` |
| `/upload` | ResourceUpload | ResourceUpload.vue | `upload` |
| `/projects/:id/package` | PackageCenter | PackageCenter.vue | `null` |

---

## 4. 后端设计规范

### 4.1 项目结构

```
deploy-manager-backend/
├── pom.xml
├── src/main/java/com/company/deploy/
│   ├── DeployManagerApplication.java
│   ├── config/
│   │   ├── MinioConfig.java
│   │   └── WebConfig.java              (CORS)
│   ├── controller/
│   │   ├── ProjectController.java      (/api/projects)
│   │   ├── FileController.java         (/api/files)
│   │   ├── InfrastructureController.java (/api/infra)
│   │   └── PackageController.java      (/api/package, /api/packages)
│   ├── service/
│   │   ├── ProjectService.java
│   │   ├── FileService.java
│   │   ├── InfrastructureService.java
│   │   ├── PackageService.java         (打包核心，@Async 异步)
│   │   └── ScriptTemplateService.java  (FreeMarker 脚本生成)
│   ├── mapper/
│   │   ├── ProjectMapper.java
│   │   ├── UploadedFileMapper.java
│   │   ├── PackageRecordMapper.java
│   │   └── PackageTaskMapper.java
│   ├── entity/
│   │   ├── Project.java
│   │   ├── UploadedFile.java
│   │   ├── PackageRecord.java
│   │   └── PackageTask.java
│   ├── dto/
│   │   ├── ProjectCreateRequest.java
│   │   ├── ProjectConfigDTO.java
│   │   ├── PackageStartRequest.java
│   │   ├── PackageProgressDTO.java
│   │   └── FileTreeNode.java
│   ├── enums/
│   │   ├── ProjectStatus.java          (CONFIGURING/READY/ARCHIVED)
│   │   ├── FileTypeEnum.java           (jar/vue/jdk/mysql/minio/nginx/engine)
│   │   └── PackageStatus.java          (PENDING/RUNNING/SUCCESS/FAILED)
│   └── common/
│       ├── Result.java                 (统一响应 {code, message, data})
│       └── PageResult.java             (分页响应 {records, total, pageNum, pageSize})
├── src/main/resources/
│   ├── application.yml
│   ├── logback-spring.xml              (日志配置)
│   ├── db/
│   │   ├── schema.sql                  (建表脚本)
│   │   └── migration.sql               (增量迁移)
│   └── scripts/templates/              (FreeMarker 脚本模板)
│       ├── start.ps1.ftl
│       ├── stop.ps1.ftl
│       └── services/
│           ├── start-jar.ps1.ftl
│           ├── stop-jar.ps1.ftl
│           ├── start-mysql.ps1.ftl
│           ├── stop-mysql.ps1.ftl
│           ├── start-minio.ps1.ftl
│           ├── stop-minio.ps1.ftl
│           ├── start-nginx.ps1.ftl
│           ├── stop-nginx.ps1.ftl
│           ├── start-engine.ps1.ftl
│           └── stop-engine.ps1.ftl
```

### 4.2 API 端点总览

#### 项目管理 `/api/projects`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/projects` | 分页查询（pageNum, pageSize, keyword） |
| GET | `/api/projects/stats` | 统计数据（totalProjects, readyProjects, configuringProjects, archivedProjects, totalPackages） |
| GET | `/api/projects/{id}` | 项目详情 |
| POST | `/api/projects` | 创建项目 |
| PUT | `/api/projects/{id}` | 更新项目 |
| DELETE | `/api/projects/{id}` | 删除项目（逻辑删除） |
| GET | `/api/projects/{id}/config` | 获取项目配置 |
| POST | `/api/projects/{id}/config` | 保存项目配置 |
| GET | `/api/projects/{id}/latest-package` | 获取最新成功打包记录 |
| GET | `/api/projects/{id}/latest-package/download` | 下载最新打包产物 |

#### 文件管理 `/api/files`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/files/tree` | 文件树（projectId 可选） |
| GET | `/api/files/list` | 文件列表（projectId, type, date） |
| GET | `/api/files/paths` | 路径选项（projectId, type） |
| POST | `/api/files/upload` | 上传单文件（file + projectId + type + date + versionTag） |
| POST | `/api/files/upload-folder` | 上传文件夹（files[] + projectId + type + date + folderName + versionTag） |
| DELETE | `/api/files/{id}` | 删除文件 |

#### 基础组件 `/api/infra`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/infra/{type}/list` | 列出基础组件（type: jdk/mysql/minio/nginx/engine） |
| POST | `/api/infra/{type}/upload` | 上传单文件组件（file + version + tag + initState + belongsTo） |
| POST | `/api/infra/{type}/upload-folder` | 上传文件夹组件（files[] + folderName + version + tag + initState + belongsTo） |
| DELETE | `/api/infra/{type}/{id}` | 删除基础组件 |

#### 打包 `/api/package` `/api/packages`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/package/start` | 启动打包（projectId + password → taskId） |
| GET | `/api/package/progress/{taskId}` | 查询进度（status/progress/currentStep/logs[]） |
| GET | `/api/packages` | 产物列表（projectId, status, pageNum, pageSize） |
| GET | `/api/packages/{id}/download` | 下载产物 |
| DELETE | `/api/packages/{id}` | 删除产物 |

### 4.3 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

分页响应：
```json
{
  "code": 200,
  "data": {
    "records": [...],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

### 4.4 关键配置（application.yml）

```yaml
server:
  port: 9090

spring:
  application:
    name: deploy-manager
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  datasource:
    url: jdbc:mysql://localhost:3306/deploy_manager?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:root}
  servlet:
    multipart:
      max-file-size: 500MB
      max-request-size: 500MB
  freemarker:
    template-loader-path: classpath:/scripts/templates/
    suffix: .ftl
    charset: UTF-8

mybatis-plus:
  mapper-locations: classpath:/mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
  bucket-project-files: project-files
  bucket-infra-components: infra-components
  bucket-packages: packages

package:
  temp-dir: ${PACKAGE_TEMP_DIR:./temp/packages}
  max-concurrent-tasks: 2
```

---

## 5. 数据库设计

### 5.1 表结构概览

数据库：`deploy_manager`（MySQL 8.0，字符集 utf8mb4）

#### project — 项目表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT, PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(100), NOT NULL, UNIQUE | 项目名称 |
| description | VARCHAR(500) | 项目描述 |
| status | VARCHAR(20), DEFAULT 'CONFIGURING' | CONFIGURING/READY/ARCHIVED |
| jar_file_id | BIGINT | 选中的 JAR 包文件 ID |
| vue_folder_id | BIGINT | 选中的 Vue 产物文件夹 ID |
| nginx_conf_file_id | BIGINT | Nginx 配置文件 ID（预留） |
| nginx_conf_content | TEXT | nginx.conf 编辑器内容 |
| app_port | INT, DEFAULT 8080 | 应用端口 |
| jvm_params | VARCHAR(500), DEFAULT '-Xms512m -Xmx2048m' | JVM 参数 |
| mysql_port | INT, DEFAULT 3306 | MySQL 端口 |
| db_name | VARCHAR(100) | 数据库名 |
| db_username | VARCHAR(100) | 数据库用户名 |
| db_password | VARCHAR(200) | 数据库密码 |
| minio_api_port | INT, DEFAULT 9000 | MinIO API 端口 |
| minio_console_port | INT, DEFAULT 9001 | MinIO Console 端口 |
| minio_access_key | VARCHAR(100) | MinIO AccessKey |
| minio_secret_key | VARCHAR(200) | MinIO SecretKey |
| nginx_http_port | INT, DEFAULT 80 | Nginx HTTP 端口 |
| engine_enabled | TINYINT(1), DEFAULT 0 | 是否包含引擎 |
| engine_port | INT, DEFAULT 8090 | 引擎端口 |
| include_jdk | TINYINT(1), DEFAULT 1 | 是否包含 JDK |
| include_mysql | TINYINT(1), DEFAULT 0 | 是否包含 MySQL |
| include_minio | TINYINT(1), DEFAULT 0 | 是否包含 MinIO |
| include_nginx | TINYINT(1), DEFAULT 0 | 是否包含 Nginx |
| jdk_version | VARCHAR(50) | JDK 版本 |
| mysql_version | VARCHAR(50) | MySQL 版本 |
| mysql_config_state | VARCHAR(20), DEFAULT 'clean' | clean/configured |
| minio_version | VARCHAR(50) | MinIO 版本 |
| minio_config_state | VARCHAR(20), DEFAULT 'clean' | clean/configured |
| nginx_version | VARCHAR(50) | Nginx 版本 |
| engine_version | VARCHAR(50) | 引擎版本 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| deleted | TINYINT(1), DEFAULT 0 | 逻辑删除 |

**非数据库字段**（`@TableField(exist = false)`）：
- `jarFileName` — JAR 包文件名（列表展示用）
- `vueConfigured` — Vue 是否已配置（列表展示用）

#### uploaded_file — 上传文件表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT, PK, AUTO_INCREMENT | 主键 |
| project_id | BIGINT | 所属项目 ID（基础组件为空） |
| file_type | VARCHAR(20), NOT NULL | jar/vue/jdk/mysql/minio/nginx/engine |
| file_name | VARCHAR(255), NOT NULL | 文件/文件夹名称 |
| file_size | BIGINT | 文件大小（字节） |
| minio_bucket | VARCHAR(100) | MinIO 存储桶 |
| minio_object_key | VARCHAR(500) | MinIO 对象键 |
| is_folder | TINYINT(1), DEFAULT 0 | 是否为文件夹 |
| folder_path | VARCHAR(500) | 文件夹路径 |
| upload_date | DATE | 上传日期 |
| version_tag | VARCHAR(100) | 版本标签（项目文件用） |
| version | VARCHAR(50) | 版本号（基础组件用） |
| tag | VARCHAR(100) | 标签（基础组件用） |
| init_state | VARCHAR(20) | 初始化状态：clean/initialized |
| belongs_to | VARCHAR(50) | 所属系统：通用/电商平台/... |
| created_at | DATETIME | 上传时间 |
| deleted | TINYINT(1), DEFAULT 0 | 逻辑删除 |

#### package_record — 打包产物表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT, PK, AUTO_INCREMENT | 主键 |
| project_id | BIGINT | 所属项目 |
| file_name | VARCHAR(255) | ZIP 文件名 |
| file_size | BIGINT | ZIP 大小 |
| encrypted | TINYINT(1), DEFAULT 1 | 是否加密 |
| minio_bucket | VARCHAR(100) | 存储桶 |
| minio_object_key | VARCHAR(500) | 对象键 |
| status | VARCHAR(20) | SUCCESS/FAILED |
| task_id | VARCHAR(36) | 打包任务 ID |
| created_at | DATETIME | 创建时间 |
| deleted | TINYINT(1), DEFAULT 0 | 逻辑删除 |

#### package_task — 打包任务表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(36), PK | 任务 ID（UUID） |
| project_id | BIGINT | 所属项目 |
| status | VARCHAR(20), DEFAULT 'PENDING' | PENDING/RUNNING/SUCCESS/FAILED |
| progress | INT, DEFAULT 0 | 进度 0-100 |
| current_step | VARCHAR(200) | 当前步骤 |
| password | VARCHAR(255) | ZIP 密码 |
| created_at | DATETIME | 创建时间 |
| finished_at | DATETIME | 完成时间 |

> 注意：`package_task` 表无 `deleted` 字段（不做逻辑删除）

### 5.2 MinIO 存储桶规划

| 存储桶 | 用途 | 文件路径规则 |
|--------|------|------------|
| `project-files` | 项目文件（JAR/Vue） | `{projectId}/{fileType}/{fileName}` |
| `infra-components` | 基础组件 | `{fileType}/{version}/{fileName}` |
| `packages` | 打包产物 | `{projectId}/{taskId}/{fileName}` |

---

## 6. 业务规则

### 6.1 项目文件管理

1. 项目文件按**两级目录**组织：`项目名 / 类型(JAR包 | Vue产物)`（移除日期层级，用版本标签区分）
2. 上传时需填写**版本标签**（如 v2.1.0）
3. 同版本标签的文件覆盖同名文件（前端警告提示）
4. JAR 包为单文件上传，Vue 产物为文件夹上传

### 6.2 基础组件管理

1. 基础组件支持**多版本管理**，每个组件可上传多个版本
2. 每个版本包含：版本号、标签、初始化状态、所属系统
3. **初始化状态**两类：
   - 纯净版（clean）：打包时由系统注入项目配置中的端口和账密
   - 含初始化数据（initialized）：包内预置配置/数据，系统不再覆盖
4. **所属系统**：通用版本所有项目可选；专属版本仅对应项目可选
5. JDK / Nginx / 引擎仅支持纯净版
6. JDK / Nginx / 引擎本身即为通用组件，无需区分所属系统
7. MySQL / MinIO 支持纯净版与含初始化数据两种

### 6.3 项目配置规则

1. 每个项目只有一套配置
2. 基础组件（JDK/MySQL/MinIO/Nginx）固定包含，但**版本可选**
3. 引擎为可选组件，通过复选框控制
4. 选「纯净版」MySQL/MinIO 时，可配置端口和账密
5. 选「已初始化」MySQL/MinIO 时，使用包内配置，不可修改
6. Nginx 端口和路由通过 nginx.conf 管理，不单独配置端口
7. SQL 脚本仅用于系统内 MySQL 初始化，**不打包到 ZIP 中**

### 6.4 打包规则

1. 打包中心所有配置为**只读**，不可修改
2. ZIP 加密始终开启，密码由用户输入
3. 启动脚本由后端根据配置自动生成
4. 打包为异步任务（`@Async`），前端轮询获取进度
5. 打包产物存储在 MinIO，通过接口下载
6. 产物支持下载和删除，可按状态筛选

### 6.5 项目状态规则

- 新建项目 → `CONFIGURING`
- 配置完成（JAR 已选 + 端口已填）→ `READY`
- 支持手动归档为 `ARCHIVED`
- 已归档项目不再自动变更状态

---

## 7. 打包流程设计

### 7.1 打包步骤

```
1. 验证配置完整性（JAR/组件/密码非空）
2. 创建 PackageTask 记录（PENDING 状态）
3. 异步执行（@Async）：
   a. 收集 JAR 包 → {packageDir}/app/
   b. 收集 JDK → {packageDir}/jdk/
   c. 收集 Vue 产物 + Nginx 配置 → {packageDir}/nginx/
   d. 收集 MySQL → {packageDir}/mysql/
   e. 收集 MinIO → {packageDir}/minio/
   f. [可选] 收集引擎 → {packageDir}/engine/
   g. 生成启动脚本（FreeMarker 模板渲染）
   h. 打包为加密 ZIP（Zip4j AES-256）
   i. 上传 ZIP 到 MinIO
   j. 记录 package_record
   k. 清理临时目录
```

### 7.2 进度推进

进度通过 5% → 15% → 30% → 45% → 55% → 62%（可选引擎）→ 70% → 80% → 90% → 100% 推进，每步更新 `currentStep` 和日志。

### 7.3 ZIP 包内部目录结构

```
{projectName}-release.zip
├── app/
│   └── app.jar
├── jdk/
├── mysql/
├── minio/
├── nginx/
│   ├── conf/nginx.conf
│   └── html/（Vue dist 产物）
├── engine/（可选）
├── services/
│   ├── start-jar.ps1 / stop-jar.ps1
│   ├── start-mysql.ps1 / stop-mysql.ps1
│   ├── start-minio.ps1 / stop-minio.ps1
│   ├── start-nginx.ps1 / stop-nginx.ps1
│   └── start-engine.ps1 / stop-engine.ps1（可选）
├── pid/
├── start.ps1
└── stop.ps1
```

### 7.4 关键设计点

- `startPackage` 方法**不可加 `@Transactional`**：异步线程在新事务中执行，需立即可见 task 记录
- 日志保留上限 500 条
- FreeMarker 模板变量包括：projectName, generateTime, jarFileName, appPort, jvmParams, mysqlPort, minioApiPort, minioConsolePort, nginxHttpPort, engineEnabled, enginePort, serviceCount 等

---

## 8. 非功能需求

| 项 | 要求 |
|----|------|
| 浏览器兼容 | Chrome / Edge 现代浏览器 |
| 上传限制 | 单文件最大 500MB（大文件建议 ZIP 压缩上传，后端自动解压） |
| 响应式 | 以桌面端为主（最小 1280px） |
| 日志 | 后端日志含时间戳/级别/PID/线程/logger，错误日志独立归档 |
| 兼容性 | 后端兼容 JDK 8 |

---

## 9. 与旧版 dev-spec 的主要差异

> 以下为当前规范与旧版 `deploy-manager-dev-spec.md` 的差异点，以本规范为准。

| 差异项 | 旧版 dev-spec | 当前规范 |
|--------|--------------|---------|
| 技术栈 | Java 17 + Spring Boot 3.x | **Java 8 + Spring Boot 2.7.18** |
| MyBatis-Plus | mybatis-plus-spring-boot3-starter 3.5.5 | **mybatis-plus-boot-starter 3.5.3.1** |
| MySQL 驱动 | mysql-connector-j | **mysql-connector-java 8.0.33** |
| 基础组件版本 | 固定单一版本，不可更改 | **支持多版本选择**，版本下拉 |
| 项目文件目录 | 三级（项目/类型/日期） | **两级**（项目/类型），用版本标签替代日期 |
| 基础组件属性 | 仅文件名/大小/时间 | 新增**初始化状态**(clean/initialized)、**所属系统** |
| MySQL/MinIO 账密 | 「账密已内置」不可改 | 纯净版在项目配置中设置端口和账密；已初始化版使用包内配置 |
| Nginx 配置 | 上传 nginx_conf 文件 | **nginx.conf 编辑器 Modal** 在线编辑 |
| SQL 脚本 | 打包到 ZIP | **不打包到 ZIP**，仅系统内 MySQL 初始化 |
| 项目管理操作 | 配置/打包/删除 | 新增**下载最新版本**（含提示弹窗） |
| 项目图标 | 统一图标 | **按项目类型动态图标** |
| 引擎命名 | 「公司引擎」 | **「UnSim引擎」** |
| MyBatis 日志 | StdOutImpl | **Slf4jImpl** |

---

*文档结束。开发时请对照原型 HTML 文件核对视觉细节。*
