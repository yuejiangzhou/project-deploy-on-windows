# DeployManager - 总停止脚本
# 项目: ${projectName}

$ErrorActionPreference = "SilentlyContinue"
$baseDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$servicesDir = Join-Path $baseDir "services"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  DeployManager - ${projectName}" -ForegroundColor Cyan
Write-Host "  停止所有服务..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

<#if engineEnabled>
Write-Host "停止引擎..." -ForegroundColor Yellow
& (Join-Path $servicesDir "stop-engine.ps1")
</#if>

Write-Host "停止 JAR应用..." -ForegroundColor Yellow
& (Join-Path $servicesDir "stop-jar.ps1")

<#if nginxEnabled>
Write-Host "停止 Nginx..." -ForegroundColor Yellow
& (Join-Path $servicesDir "stop-nginx.ps1")
</#if>

<#if minioEnabled>
Write-Host "停止 MinIO..." -ForegroundColor Yellow
& (Join-Path $servicesDir "stop-minio.ps1")
</#if>

<#if mysqlEnabled>
Write-Host "停止 MySQL..." -ForegroundColor Yellow
& (Join-Path $servicesDir "stop-mysql.ps1")
</#if>

Write-Host ""
Write-Host "所有服务已停止" -ForegroundColor Green
