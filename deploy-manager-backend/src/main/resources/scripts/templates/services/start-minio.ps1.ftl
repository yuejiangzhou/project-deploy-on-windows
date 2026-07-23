# 启动 MinIO
$baseDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.path)
$pidDir = Join-Path $baseDir "pid"
$minioExe = Join-Path $baseDir "minio\minio.exe"
$dataDir = Join-Path $baseDir "minio\data"

if (-not (Test-Path $dataDir)) { New-Item -ItemType Directory -Path $dataDir | Out-Null }

Write-Host "启动 MinIO (API: ${minioApiPort}, Console: ${minioConsolePort})..." -ForegroundColor DarkGray

<#if minioConfigState == "clean">
# 纯净版：注入账号密码环境变量，首次启动将以此初始化
$env:MINIO_ROOT_USER = "${minioAccessKey}"
$env:MINIO_ROOT_PASSWORD = "${minioSecretKey}"
Write-Host "  使用配置的账号密码初始化 MinIO..." -ForegroundColor DarkGray

$process = Start-Process -FilePath $minioExe `
    -ArgumentList "server", $dataDir, "--address", ":${minioApiPort}", "--console-address", ":${minioConsolePort}" `
    -WindowStyle Hidden `
    -PassThru

Start-Sleep -Seconds 3
Write-Host "  MinIO 已使用账号 ${minioAccessKey} 初始化" -ForegroundColor Green
<#else>
# 已初始化版：使用包内已有配置，不覆盖环境变量
$process = Start-Process -FilePath $minioExe `
    -ArgumentList "server", $dataDir, "--address", ":${minioApiPort}", "--console-address", ":${minioConsolePort}" `
    -WindowStyle Hidden `
    -PassThru

Start-Sleep -Seconds 3
</#if>

$pidFile = Join-Path $pidDir "minio.pid"
$process.Id | Out-File -FilePath $pidFile -Encoding UTF8
Write-Host "  PID: $($process.Id) -> $pidFile" -ForegroundColor DarkGray
