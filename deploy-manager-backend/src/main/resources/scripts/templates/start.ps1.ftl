# DeployManager - 总启动脚本
# 项目: ${projectName}
# 生成时间: ${generateTime}

$ErrorActionPreference = "Stop"
$baseDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$servicesDir = Join-Path $baseDir "services"
$pidDir = Join-Path $baseDir "pid"

if (-not (Test-Path $pidDir)) { New-Item -ItemType Directory -Path $pidDir | Out-Null }

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  DeployManager - ${projectName}" -ForegroundColor Cyan
Write-Host "  启动所有服务..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$step = 1

<#if mysqlEnabled>
Write-Host "[${step}/${serviceCount}] 启动 MySQL..." -ForegroundColor Yellow
& (Join-Path $servicesDir "start-mysql.ps1")
if ($LASTEXITCODE -ne 0) { Write-Host "MySQL 启动失败!" -ForegroundColor Red; exit 1 }
Start-Sleep -Seconds 5
$step = $step + 1
</#if>

<#if minioEnabled>
Write-Host "[${step}/${serviceCount}] 启动 MinIO..." -ForegroundColor Yellow
& (Join-Path $servicesDir "start-minio.ps1")
if ($LASTEXITCODE -ne 0) { Write-Host "MinIO 启动失败!" -ForegroundColor Red; exit 1 }
Start-Sleep -Seconds 2
$step = $step + 1
</#if>

<#if nginxEnabled>
Write-Host "[${step}/${serviceCount}] 启动 Nginx..." -ForegroundColor Yellow
& (Join-Path $servicesDir "start-nginx.ps1")
if ($LASTEXITCODE -ne 0) { Write-Host "Nginx 启动失败!" -ForegroundColor Red; exit 1 }
Start-Sleep -Seconds 1
$step = $step + 1
</#if>

<#if redisEnabled>
Write-Host "[${step}/${serviceCount}] 启动 Redis..." -ForegroundColor Yellow
& (Join-Path $servicesDir "start-redis.ps1")
if ($LASTEXITCODE -ne 0) { Write-Host "Redis 启动失败!" -ForegroundColor Red; exit 1 }
Start-Sleep -Seconds 1
$step = $step + 1
</#if>

Write-Host "[${step}/${serviceCount}] 启动 JAR应用..." -ForegroundColor Yellow
& (Join-Path $servicesDir "start-jar.ps1")
if ($LASTEXITCODE -ne 0) { Write-Host "JAR应用 启动失败!" -ForegroundColor Red; exit 1 }
$step = $step + 1

<#if engineEnabled>
Write-Host "[${step}/${serviceCount}] 启动引擎..." -ForegroundColor Yellow
& (Join-Path $servicesDir "start-engine.ps1")
if ($LASTEXITCODE -ne 0) { Write-Host "引擎 启动失败!" -ForegroundColor Red; exit 1 }
</#if>

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  所有服务启动完成!" -ForegroundColor Green
Write-Host "  JAR应用: http://localhost:${appPort}" -ForegroundColor Green
<#if nginxEnabled>
Write-Host "  Nginx前端: http://localhost:${nginxHttpPort}" -ForegroundColor Green
</#if>
<#if minioEnabled>
Write-Host "  MinIO Console: http://localhost:${minioConsolePort}" -ForegroundColor Green
</#if>
Write-Host "========================================" -ForegroundColor Green
