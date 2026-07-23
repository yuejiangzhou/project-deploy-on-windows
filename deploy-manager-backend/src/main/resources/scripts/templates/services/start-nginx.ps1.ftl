# 启动 Nginx
$baseDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$pidDir = Join-Path $baseDir "pid"
$nginxDir = Join-Path $baseDir "nginx"

Write-Host "启动 Nginx (端口: ${nginxHttpPort})..." -ForegroundColor DarkGray

$process = Start-Process -FilePath (Join-Path $nginxDir "nginx.exe") `
    -WorkingDirectory $nginxDir `
    -WindowStyle Hidden `
    -PassThru

$pidFile = Join-Path $pidDir "nginx.pid"
$process.Id | Out-File -FilePath $pidFile -Encoding UTF8
Write-Host "  PID: $($process.Id) -> $pidFile" -ForegroundColor DarkGray
