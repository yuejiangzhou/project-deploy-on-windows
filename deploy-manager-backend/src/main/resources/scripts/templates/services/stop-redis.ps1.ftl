# 停止 Redis
$baseDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.path)
$pidDir = Join-Path $baseDir "pid"
$redisDir = Join-Path $baseDir "redis"

$pidFile = Join-Path $pidDir "redis.pid"
if (Test-Path $pidFile) {
    $pid = Get-Content $pidFile
    $process = Get-Process -Id $pid -ErrorAction SilentlyContinue
    if ($process) {
        Write-Host "停止 Redis (PID: $pid)..." -ForegroundColor DarkGray
        & (Join-Path $redisDir "redis-cli.exe") -p ${redisPort} SHUTDOWN 2>&1 | Out-Null
        Start-Sleep -Seconds 2
        $process = Get-Process -Id $pid -ErrorAction SilentlyContinue
        if ($process) {
            Write-Host "  强制终止 Redis 进程..." -ForegroundColor Yellow
            $process.Kill()
        }
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}
Write-Host "Redis 已停止" -ForegroundColor DarkGray
