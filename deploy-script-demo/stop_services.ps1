# stop_services.ps1
# Stop all services by reading .pid files from ./pids/

$ScriptDir = $PSScriptRoot
if (-not $ScriptDir) { $ScriptDir = Get-Location }

$PidDir = Join-Path $ScriptDir "pids"
Write-Host "the pidDir is $PidDir"
if (-not (Test-Path $PidDir)) {
    Write-Host "No pid directory found. No services to stop." -ForegroundColor Yellow
    exit 0
}

$pidFiles = Get-ChildItem -Path $PidDir -Filter "*.pid" -ErrorAction SilentlyContinue
if (-not $pidFiles) {
    Write-Host "No PID files found. All services may already be stopped." -ForegroundColor Yellow
    exit 0
}

Write-Host "Stopping services...$pidFiles" -ForegroundColor Cyan

foreach ($file in $pidFiles) {
    $serviceName = $file.BaseName
    Write-Host "`nTry stop service $serviceName. The path is $($file.Name)" -ForegroundColor Cyan
    $pidContent = Get-Content $file.FullName -ErrorAction SilentlyContinue
    
    if ([string]::IsNullOrWhiteSpace($pidContent)) {
        Write-Host "  - PID file $($file.Name) is empty, removing." -ForegroundColor Yellow
        Remove-Item $file -Force
        continue
    }

    $targetPid = $pidContent.Trim() -as [int]
    if (-not $targetPid) {
        Write-Host "  - Invalid PID in $($file.Name), removing." -ForegroundColor Yellow
        Remove-Item $file -Force
        continue
    }

    $proc = Get-Process -Id $targetPid -ErrorAction SilentlyContinue
    if ($proc) {
        Write-Host "Stopping $serviceName (PID $targetPid) and its tree..."
        taskkill /F /T /PID $targetPid | Out-Null
        Write-Host "Stopped $serviceName"   -ForegroundColor Green
    } else {
        Write-Host "  - Process $serviceName (PID $targetPid) not running."  -ForegroundColor Yellow
    }
    
    Remove-Item $file.FullName -Force
}

Write-Host "`nAll services stopped." -ForegroundColor Green