# 启动 JAR 应用
$baseDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$pidDir = Join-Path $baseDir "pid"
$jarPath = Join-Path $baseDir "app\${jarFileName}"
$javaExe = Join-Path $baseDir "jdk\bin\java.exe"

Write-Host "启动 JAR: ${jarFileName}" -ForegroundColor DarkGray
Write-Host "  端口: ${appPort}" -ForegroundColor DarkGray
Write-Host "  JVM参数: ${jvmParams}" -ForegroundColor DarkGray

$process = Start-Process -FilePath $javaExe `
    -ArgumentList "${jvmParams}", "-jar", $jarPath, "--server.port=${appPort}" `
    -WindowStyle Hidden `
    -PassThru

$pidFile = Join-Path $pidDir "jar.pid"
$process.Id | Out-File -FilePath $pidFile -Encoding UTF8
Write-Host "  PID: $($process.Id) -> $pidFile" -ForegroundColor DarkGray
