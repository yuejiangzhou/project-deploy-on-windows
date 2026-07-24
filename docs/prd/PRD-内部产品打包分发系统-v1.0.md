# PRD — 内部产品打包分发系统

> **版本**: v1.0  
> **状态**: 待评审  
> **创建日期**: 2025-07-23  
> **作者**: 产品团队  
> **关联文档**: [现有代码分析报告](../docs/现有代码分析报告.md) | [需求规划](../docs/需求规划.md)

---

## 1. 修订历史

| 版本 | 日期 | 修订人 | 修订内容 |
|------|------|--------|----------|
| v1.0 | 2025-07-23 | PM | 初始版本，MVP 范围 |
| v1.1 | 2025-07-24 | PM | 更新：启动脚本方案（.bat + .ps1 混合）、License 生成机制明确、评审结论纳入 |

---

## 2. 背景与目标

### 2.1 业务背景

公司有多个 ToB 软件产品，销售团队需要频繁向潜在客户提供试用环境。当前流程是：

> 销售人员联系开发 → 开发手动打包各组件 → 传文件给销售 → 销售发给客户 → 客户自行配置环境

**痛点**：
- 交付周期长（天级别），销售响应慢
- 版本错配频发（开发给错 JAR 版本、前端版本不匹配）
- 客户部署困难（需自行安装 JDK/MySQL/Redis/MinIO/Nginx）
- License 到期后需反复找开发续期，流程割裂
- 无版本追溯，出了问题难以定位

### 2.2 产品目标

| 目标 | 衡量指标 | 目标值 |
|------|---------|--------|
| 缩短交付周期 | 从选择版本到下载完成的耗时 | < 5 分钟 |
| 降低交付出错率 | 版本错配导致的客诉次数 | 0 |
| 提升销售自助率 | 销售无需开发介入的比例 | ≥ 80% |
| 改善客户体验 | 客户从解压到可访问的耗时 | < 3 分钟 |

### 2.3 产品定位

面向**开发人员**和**销售人员**的内部 Web 管理平台，实现公司软件产品的**版本化组件管理、自动化打包、一键式部署分发和 License 生命周期管理**。

---

## 3. 用户故事

| ID | 角色 | 用户故事 | 优先级 |
|----|------|---------|--------|
| US-01 | 开发人员 | 作为开发，我想创建项目并定义项目包含的前端、后端和基础服务，以便统一管理打包配置 | P0 |
| US-02 | 开发人员 | 作为开发，我想上传不同版本的 JAR 包、Vue 前端产物和基础服务组件，以便版本化管理 | P0 |
| US-03 | 开发人员 | 作为开发，我想为每个基础服务（MySQL/MinIO等）定义纯净版和已初始化版，以便适配不同客户场景 | P0 |
| US-04 | 销售人员 | 作为销售，我想在 Web 上选择项目版本组合并一键下载打包产物，无需联系开发 | P0 |
| US-05 | 客户 | 作为客户，我想解压下载的 zip 包后双击 start.bat 就能运行试用系统，无需安装任何环境 | P0 |
| US-06 | 销售人员 | 作为销售，我想在 Web 上生成和续期客户 License，设定试用时长，无需联系开发 | P0 |
| US-07 | 客户 | 作为客户，我想在 License 到期后替换新 License 文件即可继续试用，无需重新部署 | P1 |
| US-08 | 开发人员 | 作为开发，我想自定义项目依赖的基础服务类型，不限于预置的 MySQL/Redis/MinIO | P1 |
| US-09 | 销售人员 | 作为销售，我想查看打包历史和下载记录，追踪哪些客户拿到了哪个版本 | P2 |
| US-10 | 管理员 | 作为管理员，我想管理用户角色和权限，区分开发上传和销售下载的权限 | P0 |

---

## 4. 功能清单

### 4.1 MVP 功能范围（P0）

| 编号 | 功能模块 | 功能点 | 复用现有代码 | 新增/改造 |
|------|---------|--------|-------------|----------|
| **F1** | 用户与权限 | 登录/登出 | ❌ | 🆕 新增 |
| **F2** | 用户与权限 | 角色管理（开发/销售/管理员） | ❌ | 🆕 新增 |
| **F3** | 用户与权限 | 路由权限控制 | ❌ | 🆕 新增 |
| **F4** | 项目管理 | 创建/编辑/删除项目 | ✅ ProjectList + ProjectController | 🔧 改造 |
| **F5** | 项目管理 | 项目列表搜索与分页 | ✅ 完整复用 | - |
| **F6** | 组件管理 | 前端产物（Vue dist）上传 | ✅ ResourceUpload | 🔧 改造 |
| **F7** | 组件管理 | 后端 JAR 包上传 | ✅ ResourceUpload | 🔧 改造 |
| **F8** | 组件管理 | 基础服务组件上传（JDK/MySQL/Redis/MinIO/Nginx/自研引擎） | ✅ InfraController | 🔧 改造 |
| **F9** | 组件管理 | 组件版本标签管理 | ✅ 基础实现 | 🔧 改造 |
| **F10** | 组件管理 | MySQL 纯净版 vs 已初始化版 | ✅ 已有概念 | 🔧 改造 |
| **F11** | 组件管理 | MySQL 安全版（禁用 skip-grant-tables） | ❌ | 🆕 新增 |
| **F12** | 组件管理 | 多 JDK 版本支持与选择 | ✅ 基础实现 | 🔧 改造 |
| **F13** | 项目配置 | 组件版本选择与组合 | ✅ ProjectConfig | 🔧 改造 |
| **F14** | 项目配置 | 端口与账密配置 | ✅ 已有 | - |
| **F15** | 项目配置 | Nginx 配置在线编辑 | ✅ 已有 | - |
| **F16** | 打包引擎 | 异步打包任务 | ✅ PackageService | 🔧 改造 |
| **F17** | 打包引擎 | ZIP 加密打包 | ✅ Zip4j | - |
| **F18** | 打包引擎 | 打包进度追踪 | ✅ PackageCenter | - |
| **F19** | 打包引擎 | Windows 一键启动脚本生成 | ✅ FreeMarker | 🔧 改造 |
| **F20** | 打包引擎 | 打包产物下载 | ✅ 已有 | - |
| **F21** | License 管理 | License 文件生成（设定试用时长） | ❌ | 🆕 新增 |
| **F22** | License 管理 | License 注入打包产物 | ❌ | 🆕 新增 |
| **F23** | License 管理 | License 续期（重新生成） | ❌ | 🆕 新增 |
| **F24** | License 管理 | License 文件下载 | ❌ | 🆕 新增 |

### 4.2 后续迭代（P1/P2）

| 编号 | 功能点 | 优先级 |
|------|--------|--------|
| F25 | 基础服务自定义扩展（非预置服务） | P1 |
| F26 | 打包历史与下载记录 | P2 |
| F27 | 打包模板保存与复用 | P2 |
| F28 | 统计看板（下载量/License 生成量） | P2 |
| F29 | 客户信息管理 | P2 |
| F30 | 增量更新（仅更新变更组件） | P3 |

---

## 5. 流程说明

### 5.1 核心业务流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         系统核心流程                                      │
│                                                                          │
│  ┌──────────────────────┐          ┌──────────────────────┐              │
│  │     开发人员           │          │     销售人员           │              │
│  ├──────────────────────┤          ├──────────────────────┤              │
│  │ 1. 登录系统            │          │ 1. 登录系统            │              │
│  │ 2. 创建项目            │          │ 2. 浏览项目版本        │              │
│  │ 3. 上传组件版本        │          │ 3. 选择版本组合        │              │
│  │    - 前端 dist        │          │ 4. 生成 License       │              │
│  │    - 后端 JAR         │          │    - 设定试用时长      │              │
│  │    - 基础服务         │          │ 5. 一键打包下载         │              │
│  │ 4. 配置项目组合       │          │ 6. 交付客户            │              │
│  └──────────┬───────────┘          └──────────┬───────────┘              │
│             │                                  │                          │
│             └──────────────┬───────────────────┘                          │
│                            ▼                                              │
│                    ┌──────────────┐                                       │
│                    │   打包系统     │                                       │
│                    ├──────────────┤                                       │
│                    │ 组装组件版本  │                                       │
│                    │ 生成启动脚本  │                                       │
│                    │ 注入 License │                                       │
│                    │ 加密打包 ZIP │                                       │
│                    └──────┬───────┘                                       │
│                           ▼                                               │
│                    ┌──────────────┐                                       │
│                    │  客户 Windows │                                       │
│                    ├──────────────┤                                       │
│                    │ 解压 ZIP     │                                       │
│                    │ 双击 start   │                                       │
│                    │ 开始试用     │                                       │
│                    │              │                                       │
│                    │ License到期: │                                       │
│                    │ 替换新License│                                       │
│                    │ 重启即���     │                                       │
│                    └──────────────┘                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 开发人员流程

```
登录 → 创建项目
         ├── 上传前端 dist（选版本号）
         ├── 上传后端 JAR（选版本号 + JDK版本）
         └── 上传基础服务（选版本号 + 类型 + 纯净版/已初始化版）
              ├── JDK
              ├── MySQL（纯净版:需配账密 | 已初始化版:包内已有）
              ├── Redis
              ├── MinIO（同MySQL逻辑）
              ├── Nginx
              └── 自研引擎
       → 配置项目
         ├── 选择��端版本
         ├── 选择后端版本
         ├── 选择各基础服务版本
         ├── 配置端口/账密
         └── 保存
       → 项目就绪（销售可打包）
```

### 5.3 销售人员流程

```
登录 → 浏览项目列表
       → 选择项目进入打包中心
         ├── 查看当前配置
         ├── 选择版本组合（如需调整）
         ├── 生成 License
         │   ├── 输入客户名称
         │   ├── 设定试用时长（天数）
         │   └── 生成 License 文件
         ├── 设置 ZIP 密码
         └── 点击「开始打包」
       → 等待打包完成
       → 下载 ZIP + License 文件
       → 交付客户

续期流程：
登录 → License 管理
       → 找到对��� License
       → 点击「续期」
       → 设定新的试用时长
       → 生成新 License 文件
       → 下载 → 发给客户替换
```

### 5.4 客户试用流程

```
解压 ZIP → 双击 start.bat
           ├── 自动启动 MySQL（如有）
           ├── 自动启动 Redis（如有）
           ├── 自动启动 MinIO（如有）
           ├── 自动启动 Nginx（如有）
           ├── 自动启动自研引擎（如有）
           ├── 自动启动后端 JAR
           └── 后端启动时校验 License
               ├── 有效 → 正常启动
               └── 无效/过期 → 拒绝启动，提示 License 问题
→ 浏览器访问 http://localhost 开始试用

License 到期后：
销售提供新 License 文件 → 替换到 license/ 目录 → 重启 start.bat
```

---

## 6. 功能需求（EARS 原则）

### 6.1 用户认证与权限

#### F1-01 用户登录

> **Ubiquitous**: The system shall require all users to authenticate before accessing any page except the login page.
>
> **Event-driven**: When a user submits valid credentials (username + password), the system shall validate them, return an access token, and redirect the user to the project list page.
>
> **Unwanted**: If the user submits invalid credentials, then the system shall display "用户名或密码错误" and remain on the login page, not revealing whether the username exists.
>
> **State-driven**: While the access token is expired (default 24 hours), the system shall redirect the user to the login page and clear local authentication state.

#### F1-02 角色与权限

> **Ubiquitous**: The system shall support three roles: ADMIN, DEVELOPER, and SALES.
>
> **State-driven**: While a user has the DEVELOPER role, the system shall grant access to project management, component upload, and project configuration pages.
>
> **State-driven**: While a user has the SALES role, the system shall grant access to project browsing, package center, and License management pages. SALES shall not access upload or configuration pages.
>
> **State-driven**: While a user has the ADMIN role, the system shall grant access to all pages including user management.

### 6.2 项目管理

#### F4-01 项目创建

> **Event-driven**: When a DEVELOPER submits a new project form (name + description), the system shall create a project record with status CONFIGURING and return the project ID.
>
> **Unwanted**: If the project name already exists, then the system shall return an error "项目名称已存在" and prevent creation.
>
> **Ubiquitous**: The system shall require project name to be 2-50 characters and description to be 0-500 characters.

#### F4-02 项目状态

> **Ubiquitous**: The system shall maintain project status as one of: CONFIGURING, READY, or ARCHIVED.
>
> **State-driven**: While a project is in CONFIGURING status, the system shall allow uploading components and configuring the project.
>
> **Event-driven**: When all required components are configured (JAR + Vue frontend selected), the system shall automatically transition the project status to READY.
>
> **Event-driven**: When a user archives a project, the system shall set status to ARCHIVED and prevent further packaging operations.

### 6.3 组件管理

#### F6-01 前端产物上传

> **Event-driven**: When a DEVELOPER uploads a Vue dist folder with a version label, the system shall store all files in MinIO under `project-files/{projectId}/vue/{version}/` and record the file metadata.
>
> **Ubiquitous**: The system shall support both single-file upload (for small dist zips) and chunked upload (for files > 100MB, 10MB per chunk).
>
> **Unwanted**: If the upload fails mid-way (network error), then the system shall retain uploaded chunks and allow resuming from the last successful chunk.

#### F7-01 后端 JAR 上传

> **Event-driven**: When a DEVELOPER uploads a JAR file with a version label and JDK version requirement, the system shall store the file in MinIO under `project-files/{projectId}/jar/{version}/` and record the associated JDK version.
>
> **Ubiquitous**: The system shall record the JDK version requirement (e.g., JDK8, JDK11, JDK17) for each JAR upload.

#### F8-01 基础服务组件上传

> **Event-driven**: When a DEVELOPER uploads a base service component (JDK/MySQL/Redis/MinIO/Nginx/Engine) with version, tags, and initialization state, the system shall store it in MinIO under `infra-components/{type}/{version}/` and record its metadata.
>
> **Ubiquitous**: The system shall support uploading base service components as compressed archives (zip/tar/tar.gz/tgz/7z) and automatically extract them after upload.

#### F10-01 MySQL 纯净版与已初始化版

> **Ubiquitous**: The system shall support two MySQL configuration states: CLEAN (no pre-initialized data) and CONFIGURED (pre-initialized with data).
>
> **State-driven**: While MySQL is in CLEAN state, the system shall require the DEVELOPER to specify port, root username, root password, and initialization SQL during project configuration.
>
> **State-driven**: While MySQL is in CONFIGURED state, the system shall use the package's built-in configuration and disable port/credential editing during project configuration.
>
> **Event-driven**: When CLEAN MySQL starts on the client machine, the generated start script shall initialize the data directory with `--initialize-insecure`, create the specified database and user, and execute the initialization SQL.

#### F11-01 MySQL 安全版

> **Ubiquitous**: The system shall provide a MySQL portable version that **disables the `--skip-grant-tables` startup option**, preventing unauthorized access bypass.
>
> **Ubiquitous**: The generated start script shall NOT contain any `--skip-grant-tables` parameter.
>
> **Event-driven**: When the MySQL start script detects an uninitialized data directory, it shall perform a secure initialization and immediately set the root password as configured.

### 6.4 打包引擎

#### F16-01 打包流程

> **Event-driven**: When a user (DEVELOPER or SALES) initiates a package task with a project ID and ZIP password, the system shall:
> 1. Validate the project is in READY status
> 2. Create a PENDING PackageTask record
> 3. Execute the packaging pipeline asynchronously
>
> **Ubiquitous**: The packaging pipeline shall execute in the following order:
> 1. Download JDK → `jdk/`
> 2. Download MySQL (if enabled) → `mysql/`
> 3. Download Redis (if enabled) → `redis/`
> 4. Download MinIO (if enabled) → `minio/`
> 5. Download Nginx + Vue dist → `nginx/`
> 6. Download Engine (if enabled) → `engine/`
> 7. Download JAR → `app/`
> 8. Generate start/stop scripts (PowerShell .ps1)
> 9. Generate application.yml with service configurations
> 10. Inject License file → `license/`
> 11. Package into AES-256 encrypted ZIP
> 12. Upload ZIP to MinIO `packages/` bucket

> **State-driven**: While a package task is RUNNING for a project, the system shall prevent starting another package task for the same project.
>
> **Unwanted**: If any step in the packaging pipeline fails, then the system shall mark the task as FAILED, record the error step, clean up temporary files, and release the concurrency slot.

#### F19-01 启动脚本生成

> **Ubiquitous**: The system shall generate Windows batch scripts (`start.bat` and `stop.bat`) using FreeMarker templates, NOT PowerShell (.ps1) — to maximize compatibility with Windows environments.
>
> **Ubiquitous**: The `start.bat` script shall start services in the following order: MySQL → Redis → MinIO → Nginx → Engine → JAR application.
>
> **Ubiquitous**: The `stop.bat` script shall stop services in reverse order: JAR → Engine → Nginx → MinIO → Redis → MySQL.
>
> **Ubiquitous**: Each service start/stop shall be managed via PID files in a `pid/` directory.
>
> **Optional**: Where MySQL is included in CLEAN state, the start script shall perform first-run initialization.

#### F20-01 打包产物

> **Ubiquitous**: The packaged ZIP file name shall follow the format: `{projectName}_{versionLabel}_{timestamp}.zip`.
>
> **Ubiquitous**: The packaged ZIP shall contain a `README.txt` file with usage instructions, port information, and License replacement guide.

### 6.5 License 管理

#### F21-01 License 生成

> **Event-driven**: When a SALES user requests License generation with customer name and trial duration (days), the system shall:
> 1. Accept customer name and trial days as input
> 2. Call the existing License generation code (to be provided by development team)
> 3. Generate an encrypted License file with expiry date
> 4. Store the License record in the database
> 5. Return the License file for download
>
> **Ubiquitous**: The system shall store each License record with: customer name, generated date, expiry date, associated project, and generation operator.
>
> **Unwanted**: If the License generation code is not available or fails, then the system shall return an error and log the failure for investigation.

#### F22-01 License 注入打包

> **Event-driven**: When the packaging pipeline reaches the License injection step, the system shall copy the generated License file into the package's `license/` directory.
>
> **Ubiquitous**: The License file shall be named `license.dat` and placed in a `license/` subdirectory at the root of the packaged folder.
>
> **Ubiquitous**: The generated `README.txt` shall include instructions on how to replace the License file for renewal.

#### F23-01 License 续期

> **Event-driven**: When a SALES user requests License renewal for an existing License record with a new trial duration, the system shall:
> 1. Call the License generation code with updated expiry date
> 2. Generate a new License file
> 3. Create a new License record linked to the original
> 4. Return the new License file for download
>
> **Ubiquitous**: License renewal shall generate a completely new License file — the customer replaces the old file in the `license/` directory and restarts the application.
>
> **State-driven**: While a License has expired, the system shall still allow renewal operations on that record.

---

## 7. 页面清单

| 页面 | 路由 | 访问角色 | 复用/新增 | 说明 |
|------|------|---------|----------|------|
| 登录页 | `/login` | 全部 | 🆕 新增 | 用户名密码登录 |
| 项目管理 | `/projects` | 开发/销售/管理员 | 🔧 改造 | 现有 ProjectList，增加角色过滤 |
| 项目配置 | `/projects/:id/config` | 开发 | 🔧 改造 | 现有 ProjectConfig，增加 License 配置区 |
| 资源上传 | `/upload` | 开发 | 🔧 改造 | 现有 ResourceUpload，增加 Redis 类型 |
| 打包中心 | `/projects/:id/package` | 开发/销售 | 🔧 改造 | 现有 PackageCenter，增加 License 生成 |
| License 管理 | `/license` | 销售/管理员 | 🆕 新增 | License 列表、生成、续期、下载 |
| 用户管理 | `/users` | 管理员 | 🆕 新增 | 用户列表、角色分配 |

---

## 8. 数据库设计

### 8.1 新增表

#### license_record（License 记录表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| project_id | BIGINT FK | 关联项目 |
| customer_name | VARCHAR(200) | 客户名称 |
| license_file | VARCHAR(500) | License 文件 MinIO 路径 |
| expire_date | DATETIME | 过期时间 |
| trial_days | INT | 试用天数 |
| status | VARCHAR(20) | ACTIVE / EXPIRED / REVOKED |
| parent_id | BIGINT FK | 续期来源 License ID（可为空） |
| generated_by | BIGINT FK | 操作人 ID |
| created_at | DATETIME | 生成时间 |
| updated_at | DATETIME | 更新时间 |

#### user（用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| username | VARCHAR(50) UNIQUE | 用户名 |
| password | VARCHAR(255) | 加密密码 |
| display_name | VARCHAR(100) | 显示名称 |
| role | VARCHAR(20) | ADMIN / DEVELOPER / SALES |
| status | VARCHAR(20) | ACTIVE / DISABLED |
| last_login | DATETIME | 最后登录时间 |
| created_at | DATETIME | 创建时间 |

### 8.2 改造表

#### uploaded_file 表新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| version | VARCHAR(50) | 版本号（如 v2.1.0） |
| version_label | VARCHAR(50) | 版本标签（stable/compatible/latest） |
| init_state | VARCHAR(20) | 初始化状态（CLEAN/CONFIGURED），MySQL/MinIO 专用 |
| belong_system | VARCHAR(50) | 所属系统（通用/项目专属） |

#### project 表新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| mysql_username | VARCHAR(100) | MySQL 用户名 |
| mysql_password | VARCHAR(255) | MySQL 密码 |
| minio_username | VARCHAR(100) | MinIO 用户名 |
| minio_password | VARCHAR(255) | MinIO 密码 |
| redis_port | INT | Redis 端口 |
| jdk_component_id | BIGINT FK | 选中的 JDK 组件 ID |
| mysql_component_id | BIGINT FK | 选中的 MySQL 组件 ID |
| redis_component_id | BIGINT FK | 选中的 Redis 组件 ID |
| minio_component_id | BIGINT FK | 选中的 MinIO 组件 ID |
| nginx_component_id | BIGINT FK | 选中的 Nginx 组件 ID |
| engine_component_id | BIGINT FK | 选中的 Engine 组件 ID |

#### project 表删除字段

| 字段 | 原因 |
|------|------|
| sql_file_id | SQL 不再单独打包，改为 MySQL 初始化 SQL 在线编辑 |

---

## 9. 接口设计

### 9.1 新增 API

#### 认证相关

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录，返回 token |
| POST | `/api/auth/logout` | 用户登出 |
| GET | `/api/auth/me` | 获取当前用户信息 |

#### License 相关

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/license/generate` | 生成 License（projectId + customerName + trialDays） |
| POST | `/api/license/{id}/renew` | 续期 License（newTrialDays） |
| GET | `/api/license/list` | License 列表（分页、可按项目/状态筛选） |
| GET | `/api/license/{id}/download` | 下载 License 文件 |

#### 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/users` | 用户列表（管理员） |
| POST | `/api/users` | 创建用户（管理员） |
| PUT | `/api/users/{id}` | 编辑用户（管理员） |
| PUT | `/api/users/{id}/password` | 重置密码 |

### 9.2 改造 API

| 方法 | 路径 | 改造内容 |
|------|------|---------|
| POST | `/api/package/start` | 增加 licenseId 参数，注入 License 到打包产物 |
| GET | `/api/projects` | 增加角色过滤（销售只能看到 READY 项目） |
| POST | `/api/projects/{id}/config` | 增加 Redis、License 相关配置字段 |

---

## 10. 打包产物目录结构

```
{projectName}_v{version}_{timestamp}/
├── start.bat                    ← 一键启动（批量脚本）
├── stop.bat                     ← 一键停止
├── README.txt                   ← 使用说明 + License 替换指南
├── jdk/                         ← JDK 便携版
│   └── bin/java.exe
├── app/
│   ├── app.jar                  ← 后端 JAR
│   └── config/
│       └── application.yml      ← 运行时配置
├── nginx/
│   ├── nginx.exe
│   ├── conf/nginx.conf
│   └── html/                    ← 前端 dist 产物
├── mysql/                       ← MySQL 绿色版（如包含）
│   ├── bin/
│   ├── data/                    ← 数据目录（已初始化版包含）
│   └── init.sql                 ← 初始化 SQL（纯净版）
├── redis/                       ← Redis（如包含）
│   ├── redis-server.exe
│   └── redis.conf
├── minio/                       ← MinIO（如包含）
│   └── minio.exe
├── engine/                      ← 自研引擎（如包含）
├── license/
│   └── license.dat              ← License 文件（客户替换此文件续期）
└── pid/                         ← 运行时 PID 文件
```

---

## 11. 非功能需求

### 11.1 性能

| 指标 | 要求 |
|------|------|
| 打包速度 | 10GB 以内组件，打包完成 < 5 分钟 |
| 并发打包 | 支持同时 2 个打包任务 |
| 文件上传 | 支持 10GB 单文件分片上传 |
| 页面响应 | 列表页首屏加载 < 2 秒 |

### 11.2 安全

| 要求 | 说明 |
|------|------|
| 密码加密存储 | 用户密码 BCrypt 加密 |
| Token 管理 | JWT Token，24 小时过期 |
| ZIP 加密 | AES-256 加密 |
| MySQL 安全 | 禁用 skip-grant-tables |
| License 加密 | 使用已有 License 加密方案 |
| 传输安全 | 生产环境强制 HTTPS |

### 11.3 兼容性

| 维度 | 要求 |
|------|------|
| 浏览器 | Chrome 90+, Edge 90+ |
| 服务端 | Linux (Docker 部署) |
| 客户端 | Windows 10/11 x64 |
| JDK 版本 | JDK8, JDK11, JDK17 便携版 |

---

## 12. 验收标准

### 12.1 开发人员验收

| 编号 | 验收标准 | 验证方式 |
|------|---------|---------|
| AC-01 | 能创建项目并上传至少 3 个版本的 JAR 包 | 手动测试 |
| AC-02 | 能上传 Vue dist 产物并正确关联到项目 | 手动测试 |
| AC-03 | 能上传 MySQL 纯净版和已初始化版两种 | 手动测试 |
| AC-04 | 能上传 Redis、MinIO、Nginx、自研引擎等基础服务 | 手动测试 |
| AC-05 | 能选择各组件版本组合并保存配置 | 手动测试 |
| AC-06 | 项目配置完成后状态自动变为 READY | 手动测试 |

### 12.2 销售人员验收

| 编号 | 验收标准 | 验证方式 |
|------|---------|---------|
| AC-07 | 能浏览 READY 状态的项目列表 | 手动测试 |
| AC-08 | 能在打包中心生成 License（输入客户名+天数） | 手动测试 |
| AC-09 | 能启动打包任务并看到实时进度 | 手动测试 |
| AC-10 | 打包完成后能下载加密 ZIP 和 License 文件 | 手动测试 |
| AC-11 | 能在 License 管理页对已有 License 续期 | 手动测试 |

### 12.3 客户验收

| 编号 | 验收标准 | 验证方式 |
|------|---------|---------|
| AC-12 | Windows 解压后双击 start.bat，所有服务正常启动 | Windows 环境测试 |
| AC-13 | 浏览器访问 http://localhost 能正常打开前端页面 | 手动测试 |
| AC-14 | License 过期后，替换新 License 文件重启后恢复正常 | 手动测试 |
| AC-15 | MySQL 纯净版首次启动自动初始化数据库 | 手动测试 |
| AC-16 | MySQL 已初始化版直接使用包内数据启动 | 手动测试 |

### 12.4 权限验收

| 编号 | 验收标准 | 验证方式 |
|------|---------|---------|
| AC-17 | 销售角色无法访问上传页和配置页 | 手动测试 |
| AC-18 | 未登录用户自动跳转登录页 | 手动测试 |
| AC-19 | Token 过期后自动跳转登录页 | 手动测试 |

---

## 13. 风险与依赖

| 风险/依赖 | 影响 | 缓解措施 |
|-----------|------|---------|
| License 生成代码未交付 | License 模块无法联调 | 先做 UI 和接口预留，License 生成接口 mock |
| MySQL 安全版绿色包未准备 | 无法验证安全启动 | 开发期间先使用普通版，安全版并行调研 |
| 打包体积大（GB 级）下载慢 | 销售体验差 | 分片下载、断点续传、内网部署 |
| Windows 便携版基础服务兼容性 | 部分服务在 Windows 上不稳定 | 逐个服务验证，备选 Docker 方案 |
| JDK 多版本兼容性问题 | JAR 与 JDK 版本不匹配 | 上传时要求标注 JDK 版本，打包时校验 |

---

## 14. 附录

### 14.1 术语表

| 术语 | 说明 |
|------|------|
| 纯净版 | 不含预初始化数据的基础服务包，首次启动时自动初始化 |
| 已初始化版 | 包含预置数据和配置的基础服务包，启动即可用 |
| 安全版 MySQL | 禁用 skip-grant-tables 的 MySQL 绿色版 |
| 组件版本 | 单个组件（如 JAR、JDK）的具体版本号 |
| 打包产物 | 最终生成的加密 ZIP 文件 |

### 14.2 参考文档

- 现有需求文档：`deploy-manager-requirements.md`
- 现有代码分析报告：`/workspace/pack-system/docs/现有代码分析报告.md`
