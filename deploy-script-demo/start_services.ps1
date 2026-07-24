# start_services.ps1
# Start all services in order: MySQL -> MinIO -> UnSimE -> mms-server -> mms-web
# Write each PID to .pid file under ./pids/
# If any service fails, stop all started services and forcibly clean up remaining processes.

# ===== YOUR ENVIRONMENT CONFIGURATION =====
$MySQLPort     = 3606
$MinIOPort     = 9100
$MMSServerPort = 9004
$MMSProfile    = "local"
$UnSimEProcessName = "startup"
$MySQLRootPwd  = "unsimserver"

# ===== FUNCTION DEFINITIONS =====
function Test-PortOpen {
    param($Port, $Timeout = 30)
    $start = Get-Date
    while ((Get-Date) - $start -lt (New-TimeSpan -Seconds $Timeout)) {
        $conn = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
        if ($conn -and $conn.State -eq 'Listen') { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Write-PidFile {
    param($ServiceName, $ProcessId)
    $file = Join-Path $PidDir "$ServiceName.pid"
    $ProcessId | Out-File -FilePath $file -Force
    if (-not (Test-Path $file)) {
        throw "Failed to write PID file for $ServiceName"
    }
    $content = Get-Content $file -ErrorAction SilentlyContinue
    if ($content -ne $ProcessId) {
        throw "PID file content mismatch for $ServiceName"
    }
}

function Remove-PidFile {
    param($ServiceName)
    $file = Join-Path $PidDir "$ServiceName.pid"
    if (Test-Path $file) { Remove-Item $file -Force }
}

# Strong cleanup function: kill by PID if available, then by process name as fallback
function Force-Cleanup {
    param($Services)
    Write-Host "`n[ERROR] Performing forceful cleanup of all services..." -ForegroundColor Red

    # 1. Stop by PID if services list provided
    foreach ($svc in $Services) {
        if ($svc.ProcessId) {
            Stop-Process -Id $svc.ProcessId -Force -ErrorAction SilentlyContinue
            Remove-PidFile $svc.Name
            Write-Host "Stopped $($svc.Name) (PID $($svc.ProcessId))"
        }
    }

    # 2. Fallback: kill by process name for known services
    Write-Host "Force-killing any remaining known processes..."
    # MySQL
    taskkill /F /IM mysqld.exe 2>$null | Out-Null
    Write-Host "  - Killed any remaining mysqld.exe"
    # MinIO (if it's a standalone exe, adjust name)
    taskkill /F /IM minio.exe 2>$null | Out-Null
    Write-Host "  - Killed any remaining minio.exe"
    # UnSimE
    taskkill /F /IM startup.exe 2>$null | Out-Null
    Write-Host "  - Killed any remaining startup.exe"
    # Java (mms-server) - careful if other Java apps exist, but we assume only ours
    taskkill /F /IM java.exe 2>$null | Out-Null
    Write-Host "  - Killed any remaining java.exe"
    # Node (mms-web)
    taskkill /F /IM node.exe 2>$null | Out-Null
    Write-Host "  - Killed any remaining node.exe"

    # 3. Remove all PID files
    Remove-Item "$PidDir\*.pid" -Force -ErrorAction SilentlyContinue
    Write-Host "Cleanup completed." -ForegroundColor Green
    exit 1
}


$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$JarFile = Join-Path $ScriptDir "mms-server\mms-biz.jar"
$checJavaBin = Join-Path $ScriptDir "jdk1.8.0_341\bin"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  License校验" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan


Write-Host ""
Write-Host "① Checking License..." -ForegroundColor Yellow


$licenseFile = Join-Path $ScriptDir "mms-server\license\license.lic"
if (-not (Test-Path $licenseFile)) {
    Write-Host "ERROR: License file not found " -ForegroundColor Red
    Write-Host "Please place your license.lic file in the 'license' folder." -ForegroundColor Red
    Read-Host "`nPress Enter to exit..."
    exit 1
}

$runtimeDir = Join-Path $ScriptDir "mms-server\.runtime"
if (-not (Test-Path $runtimeDir)) {
    Write-Host "ERROR: System key file not found!!!" -ForegroundColor Red
    exit 1
}


Write-Host "   Running license validation (pre-check)..." -ForegroundColor Gray
$checkArgs = "-jar  `"$JarFile`" --validate-only"
#$checkProc = Start-Process -FilePath "$checJavaBin\java.exe" ` -ArgumentList $checkArgs -WorkingDirectory (Split-Path $JarFile) `  -Wait -PassThru -WindowStyle Hidden
$workingDir = Split-Path $JarFile

# 定义日志文件路径（置于同级目录）
$stdoutLog = Join-Path $workingDir "stdout.log"
$stderrLog = Join-Path $workingDir "stderr.log"

# 启动进程并重定向输出
$checkProc = Start-Process -FilePath "$checJavaBin\java.exe" `
    -ArgumentList $checkArgs `
    -WorkingDirectory $workingDir `
    -Wait -PassThru `
    -WindowStyle Hidden `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog

if ($checkProc.ExitCode -ne 0) {
    Write-Host "License validation FAILED. Services will NOT be started." -ForegroundColor Red
    Write-Host "   请联系虚幻信息科技陈先生 13051650026，重新申请license. " -ForegroundColor Red
    Read-Host "`nPress Enter to exit..."
    exit 1
}

Write-Host "License validation PASSED." -ForegroundColor Green

# ===== SCRIPT START =====
$ScriptDir = $PSScriptRoot
if (-not $ScriptDir) { $ScriptDir = Get-Location }

# Ensure PID directory exists
$PidDir = Join-Path $ScriptDir "pids"
if (-not (Test-Path $PidDir)) { New-Item -ItemType Directory -Path $PidDir -Force | Out-Null }

# Pre-clean: kill any stray MySQL process to free port
Write-Host "Pre-cleaning any existing MySQL processes..." -ForegroundColor Yellow
taskkill /F /IM mysqld.exe 2>$null | Out-Null
Start-Sleep -Seconds 1

# Paths
$MySQLBin   = Join-Path $ScriptDir "mysql\bin"
$MySQLBase  = Join-Path $ScriptDir "mysql"
$MySQLData  = Join-Path $MySQLBase "data"
$MySQLIni   = Join-Path $MySQLBase "my.ini"
$MinIOBat   = Join-Path $ScriptDir "minio\bin\start_minio.bat"
$UnSimEExe  = Join-Path $ScriptDir "UnSimE\startup.exe"
$MMSServerJar = Join-Path $ScriptDir "mms-server\mms-biz.jar"
$MMSWebDir  = Join-Path $ScriptDir "mms-server"
$MMSWebDist = Join-Path $MMSWebDir "dist"

$startedServices = @()

# ---------- 1. MySQL ----------
Write-Host "`n[1/5] Setting up MySQL on port $MySQLPort ..." -ForegroundColor Cyan

$mysqldExe = Join-Path $MySQLBin "mysqld.exe"
if (-not (Test-Path $mysqldExe)) {
    Write-Host "ERROR: mysqld.exe not found at $mysqldExe" -ForegroundColor Red
    Force-Cleanup $startedServices
}

$MySQLBaseAbs = Resolve-Path $MySQLBase
$MySQLDataAbs = Join-Path $MySQLBaseAbs "data"
Write-Host "Generating my.ini at $MySQLIni ..."
@"
[mysqld]
port=$MySQLPort
basedir="$MySQLBaseAbs"
datadir="$MySQLDataAbs"
max_connections=200
character-set-server=utf8
default-storage-engine=INNODB

[mysql]
default-character-set=utf8
"@ | Out-File -FilePath $MySQLIni -Encoding default

if (-not (Test-Path $MySQLDataAbs)) {
    Write-Host "Initializing MySQL data directory (may take 10-20 seconds) ..."
    $initProc = Start-Process -FilePath $mysqldExe `
        -ArgumentList "--initialize-insecure" `
        -WorkingDirectory $MySQLBin `
        -PassThru -WindowStyle Hidden -Wait
    if ($initProc.ExitCode -ne 0) {
        Write-Host "ERROR: MySQL initialization failed (exit $($initProc.ExitCode))." -ForegroundColor Red
        Force-Cleanup $startedServices
    }
    Write-Host "Initialization completed."
}

Write-Host "Starting MySQL daemon ..."
$mysqlProc = Start-Process -FilePath $mysqldExe `
    -WorkingDirectory $MySQLBin `
    -PassThru -WindowStyle Hidden

Start-Sleep -Seconds 3
$startTime = Get-Date
$timeoutSeconds = 10

while ((Get-Date) -lt $startTime.AddSeconds($timeoutSeconds)) {
    if (-not $mysqlProc.HasExited) {
        break 
    }
    Start-Sleep -Milliseconds 500 
}

if ($mysqlProc.HasExited) {
    Write-Host "ERROR: MySQL process exited or failed to start within 10s. Check $MySQLDataAbs\*.err" -ForegroundColor Red
    Force-Cleanup $startedServices
}

if (-not (Test-PortOpen -Port $MySQLPort -Timeout 30)) {
    Write-Host "ERROR: MySQL port $MySQLPort not listening after 30s." -ForegroundColor Red
    Stop-Process -Id $mysqlProc.Id -Force -ErrorAction SilentlyContinue
    Force-Cleanup $startedServices
}
Write-Host "MySQL is up."

$mysqlExe = Join-Path $MySQLBin "mysql.exe"
if (-not (Test-Path $mysqlExe)) {
    Write-Host "ERROR: mysql.exe not found. Cannot set password." -ForegroundColor Red
    Force-Cleanup $startedServices
}

# Set password
$setPwd = Start-Process -FilePath $mysqlExe `
    -ArgumentList "-u root --port=$MySQLPort -e `"ALTER USER 'root'@'localhost' IDENTIFIED BY '$MySQLRootPwd';`"" `
    -WorkingDirectory $MySQLBin `
    -PassThru -WindowStyle Hidden -Wait

if ($setPwd.ExitCode -ne 0) {
    $testConn = Start-Process -FilePath $mysqlExe `
        -ArgumentList "-u root -p$MySQLRootPwd --port=$MySQLPort -e `"select 1`"" `
        -WorkingDirectory $MySQLBin `
        -PassThru -WindowStyle Hidden -Wait
    if ($testConn.ExitCode -ne 0) {
        Write-Host "ERROR: Failed to set root password and cannot connect with new password." -ForegroundColor Red
        Force-Cleanup $startedServices
    } else {
        Write-Host "Root password already set correctly." -ForegroundColor Green
    }
} else {
    Write-Host "Root password set successfully." -ForegroundColor Green
}

$mysqlPid = $mysqlProc.Id
try {
    Write-PidFile "mysql" $mysqlPid
    $startedServices += @{ Name = "mysql"; ProcessId = $mysqlPid }
    Write-Host "MySQL started (PID $mysqlPid)." -ForegroundColor Green
} catch {
    Write-Host "ERROR: Failed to write PID file for MySQL: $_" -ForegroundColor Red
    Force-Cleanup $startedServices
}

# ---------- 2. MinIO ----------
Write-Host "`n[2/5] Starting MinIO on port $MinIOPort ..." -ForegroundColor Cyan
$minioWorkDir = Split-Path $MinIOBat
$minioProc = Start-Process -FilePath $MinIOBat `
    -WorkingDirectory $minioWorkDir `
    -PassThru -WindowStyle Hidden

Start-Sleep -Seconds 3
$minioPid = $null
$conn = Get-NetTCPConnection -LocalPort $MinIOPort -ErrorAction SilentlyContinue | Where-Object { $_.State -eq 'Listen' }
if ($conn) { $minioPid = $conn.OwningProcess }

if ($minioPid) {
    try {
        Write-PidFile "minio" $minioPid
        $startedServices += @{ Name = "minio"; ProcessId = $minioPid }
        Write-Host "MinIO started (PID $minioPid)." -ForegroundColor Green
    } catch {
        Write-Host "ERROR: Failed to write PID file for MinIO: $_" -ForegroundColor Red
        Stop-Process -Id $minioProc.Id -Force -ErrorAction SilentlyContinue
        Force-Cleanup $startedServices
    }
} else {
    Write-Host "ERROR: MinIO port not listening." -ForegroundColor Red
    Stop-Process -Id $minioProc.Id -Force -ErrorAction SilentlyContinue
    Force-Cleanup $startedServices
}

# ---------- 3. UnSimE ----------
Write-Host "`n[3/5] Starting UnSimE ..." -ForegroundColor Cyan
$unsimeWorkDir = Split-Path $UnSimEExe
$unsimeProc = Start-Process -FilePath $UnSimEExe `
    -WorkingDirectory $unsimeWorkDir `
    -PassThru -WindowStyle Hidden

Start-Sleep -Seconds 3
if ($unsimeProc.HasExited) {
    Write-Host "ERROR: UnSimE process exited immediately." -ForegroundColor Red
    Force-Cleanup $startedServices
}
$unsimePid = $unsimeProc.Id
if (Get-Process -Id $unsimePid -ErrorAction SilentlyContinue) {
    try {
        Write-PidFile "unsime" $unsimePid
        $startedServices += @{ Name = "unsime"; ProcessId = $unsimePid }
        Write-Host "UnSimE started (PID $unsimePid)." -ForegroundColor Green
    } catch {
        Write-Host "ERROR: Failed to write PID file for UnSimE: $_" -ForegroundColor Red
        Stop-Process -Id $unsimePid -Force -ErrorAction SilentlyContinue
        Force-Cleanup $startedServices
    }
} else {
    Write-Host "ERROR: UnSimE process not found." -ForegroundColor Red
    Force-Cleanup $startedServices
}

# ---------- 4. mms-server ----------
Write-Host "`n[4/5] Starting mms-server on port $MMSServerPort (profile=$MMSProfile) ..." -ForegroundColor Cyan
$javaArgs = "-jar `"$MMSServerJar`" --server.port=$MMSServerPort --spring.profiles.active=$MMSProfile"
$JavaBin = Join-Path $ScriptDir "jdk1.8.0_341\bin"
$javaProc = Start-Process -FilePath "$JavaBin\java.exe" `
    -ArgumentList $javaArgs `
    -WorkingDirectory (Split-Path $MMSServerJar) `
    -PassThru -WindowStyle Hidden

Start-Sleep -Seconds 5

if ($javaProc.HasExited) {
    $exitCode = $javaProc.ExitCode
    Write-Host "ERROR: mms-server process exited immediately with code $exitCode.Please check the log file." -ForegroundColor Red
    Force-Cleanup $startedServices
}else {
    Write-Host "Waiting for mms-server run"
    
    $portOpen = $false
    $timeoutSeconds = 60
    $elapsed = 0
    $spinner = @('|', '-', '+', '*','\','/',"?")
    $i = 0
    while (-not $portOpen -and $elapsed -lt $timeoutSeconds) {
        $i = $i + 1
        Write-Host -NoNewline "`rWaiting... $($spinner[$i % 7])"
        if (Test-PortOpen -Port $MMSServerPort -Timeout 1) {
            $portOpen = $true
        } else {
            $elapsed += 0.5
        }
    }

    if ($portOpen) {
        Write-Host "`nPort $MMSServerPort is ready!"
        $javaPid = $javaProc.Id
        try {
            Write-PidFile "mmsserver" $javaPid
            $startedServices += @{ Name = "mmsserver"; ProcessId = $javaPid }
            Write-Host "mms-server started (PID $javaPid)." -ForegroundColor Green
        } catch {
            Write-Host "ERROR: Failed to write PID file for mms-server: $_" -ForegroundColor Red
            Stop-Process -Id $javaPid -Force -ErrorAction SilentlyContinue
            Force-Cleanup $startedServices
        }
    } else {
        Write-Host "`nERROR: mms-server port not listening within $timeoutSeconds seconds." -ForegroundColor Red
        Stop-Process -Id $javaProc.Id -Force -ErrorAction SilentlyContinue
        Force-Cleanup $startedServices
    }
}

# ---------- 5. nginx ----------
Write-Host "`n[5/5] Starting Nginx on port" -ForegroundColor Cyan


$nginxPath = ".\nginx\nginx.exe" 
$nginxArgs = "-p", ".\nginx" 

try {
    $webProc = Start-Process -FilePath $nginxPath `
        -ArgumentList $nginxArgs `
        -WorkingDirectory "." `
        -PassThru -WindowStyle Hidden

    Start-Sleep -Seconds 3

    if (Test-PortOpen -Port 8080 -Timeout 30) {
        $webPid = $webProc.Id
        Write-PidFile "mmsweb" $webPid
        $startedServices += @{ Name = "mmsweb"; ProcessId = $webPid }
        Write-Host "Nginx started successfully (PID $webPid)." -ForegroundColor Green
    } else {
        throw "Port not open after starting Nginx."
    }
} catch {
    Write-Host "ERROR: Failed to start Nginx: $_" -ForegroundColor Red
    if ($webProc) { Stop-Process -Id $webProc.Id -Force -ErrorAction SilentlyContinue }
    Force-Cleanup $startedServices
}

Write-Host "`nAll services started successfully!" -ForegroundColor Green

Write-Host "`n系统访问地址: http:\\localhost:8080"  -ForegroundColor Blue

Write-Host "`n按 [任意键] 继续...将为用户自动打开浏览器" -ForegroundColor Yellow
Read-Host 
Start-Process "http://localhost:8080"
