# 停止 MinIO
$baseDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$pidDir = Join-Path $baseDir "pid"
$pidFile = Join-Path $pidDir "minio.pid"

if (Test-Path $pidFile) {
    $pid = Get-Content $pidFile -Raw
    $pid = $pid.Trim()
    Write-Host "停止 MinIO (PID: $pid)..." -ForegroundColor DarkGray
    Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
    Remove-Item $pidFile -Force
    Write-Host "  已停止" -ForegroundColor Green
} else {
    Write-Host "PID文件不存在，跳过" -ForegroundColor Yellow
}
