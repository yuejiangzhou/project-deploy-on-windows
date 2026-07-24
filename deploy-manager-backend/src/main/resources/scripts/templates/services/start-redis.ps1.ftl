# 启动 Redis
$baseDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.path)
$pidDir = Join-Path $baseDir "pid"
$redisDir = Join-Path $baseDir "redis"
$redisConf = Join-Path $redisDir "redis.conf"

Write-Host "启动 Redis (端口: ${redisPort})..." -ForegroundColor DarkGray

# 生成 redis.conf
$redisConfContent = @"
port ${redisPort}
bind 127.0.0.1
protected-mode yes
daemonize no
pidfile $($pidDir -replace '\\', '/')/redis.pid
logfile $($redisDir -replace '\\', '/')/redis.log
dir $($redisDir -replace '\\', '/')
save 900 1
save 300 10
save 60 10000
dbfilename dump.rdb
appendonly yes
appendfilename "appendonly.aof"
"@
$redisConfContent | Out-File -FilePath $redisConf -Encoding UTF8 -Force

$process = Start-Process -FilePath (Join-Path $redisDir "redis-server.exe") `
    -ArgumentList "`"$redisConf`"" `
    -WindowStyle Hidden `
    -PassThru

Start-Sleep -Seconds 2

$pidFile = Join-Path $pidDir "redis.pid"
$process.Id | Out-File -FilePath $pidFile -Encoding UTF8
Write-Host "  PID: $($process.Id) -> $pidFile" -ForegroundColor DarkGray
