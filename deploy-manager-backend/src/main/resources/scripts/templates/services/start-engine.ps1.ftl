# 启动引擎
$baseDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$pidDir = Join-Path $baseDir "pid"
$engineDir = Join-Path $baseDir "engine"
$javaExe = Join-Path $baseDir "jdk\bin\java.exe"

Write-Host "启动引擎 (端口: ${enginePort})..." -ForegroundColor DarkGray

$engineJar = Get-ChildItem -Path $engineDir -Filter "*.jar" | Select-Object -First 1
if ($engineJar) {
    $process = Start-Process -FilePath $javaExe `
        -ArgumentList "-jar", $engineJar.FullName, "--server.port=${enginePort}" `
        -WindowStyle Hidden `
        -PassThru

    $pidFile = Join-Path $pidDir "engine.pid"
    $process.Id | Out-File -FilePath $pidFile -Encoding UTF8
    Write-Host "  PID: $($process.Id) -> $pidFile" -ForegroundColor DarkGray
} else {
    Write-Host "  未找到引擎 JAR 文件!" -ForegroundColor Red
    exit 1
}
