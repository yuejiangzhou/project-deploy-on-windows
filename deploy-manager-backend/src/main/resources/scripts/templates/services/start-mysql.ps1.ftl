# 启动 MySQL
$baseDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.path)
$pidDir = Join-Path $baseDir "pid"
$mysqlDir = Join-Path $baseDir "mysql"
$dataDir = Join-Path $mysqlDir "data"

Write-Host "启动 MySQL (端口: ${mysqlPort})..." -ForegroundColor DarkGray

# 生成 my.ini 配置文件
$myIni = Join-Path $mysqlDir "my.ini"
$myIniContent = @"
[mysqld]
port=${mysqlPort}
datadir=$($dataDir -replace '\\', '\\')
basedir=$($mysqlDir -replace '\\', '\\')
max_connections=200
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
default-storage-engine=INNODB
sql_mode=NO_ENGINE_SUBSTITUTION,STRICT_TRANS_TABLES
[client]
port=${mysqlPort}
default-character-set=utf8mb4
[mysql]
default-character-set=utf8mb4
"@
$myIniContent | Out-File -FilePath $myIni -Encoding UTF8 -Force

<#if mysqlConfigState == "clean">
# 纯净版：首次启动需要初始化数据目录并创建数据库/用户
if (-not (Test-Path $dataDir) -or (Get-ChildItem $dataDir -ErrorAction SilentlyContinue).Count -eq 0) {
    Write-Host "  初始化 MySQL 数据目录..." -ForegroundColor DarkGray
    & (Join-Path $mysqlDir "bin\mysqld.exe") --initialize-insecure --console 2>&1 | Out-Null
    Start-Sleep -Seconds 2
}

$process = Start-Process -FilePath (Join-Path $mysqlDir "bin\mysqld.exe") `
    -ArgumentList "--defaults-file=`"$myIni`"", "--console" `
    -WindowStyle Hidden `
    -PassThru

Start-Sleep -Seconds 5

# 等待 MySQL 就绪后注入账密
$ready = $false
for ($i = 0; $i -lt 10; $i++) {
    try {
        $test = & (Join-Path $mysqlDir "bin\mysql.exe") -u root --port=${mysqlPort} -e "SELECT 1" 2>&1
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    } catch {}
    Start-Sleep -Seconds 2
}

if ($ready) {
    Write-Host "  注入数据库和账号配置..." -ForegroundColor DarkGray
    $initSql = @"
CREATE DATABASE IF NOT EXISTS \`${dbName}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${dbUsername}'@'localhost' IDENTIFIED BY '${dbPassword}';
CREATE USER IF NOT EXISTS '${dbUsername}'@'%' IDENTIFIED BY '${dbPassword}';
GRANT ALL PRIVILEGES ON \`${dbName}\`.* TO '${dbUsername}'@'localhost';
GRANT ALL PRIVILEGES ON \`${dbName}\`.* TO '${dbUsername}'@'%';
FLUSH PRIVILEGES;
"@
    $initSqlPath = Join-Path $mysqlDir "init.sql"
    $initSql | Out-File -FilePath $initSqlPath -Encoding UTF8 -Force
    & (Join-Path $mysqlDir "bin\mysql.exe") -u root --port=${mysqlPort} -e "source $initSqlPath" 2>&1 | Out-Null
    Remove-Item $initSqlPath -Force -ErrorAction SilentlyContinue
    Write-Host "  数据库 ${dbName} 和账号 ${dbUsername} 已创建" -ForegroundColor Green
} else {
    Write-Host "  警告: MySQL 未就绪，跳过账密注入" -ForegroundColor Yellow
}
<#else>
# 已初始化版：使用包内已有配置，直接启动
$process = Start-Process -FilePath (Join-Path $mysqlDir "bin\mysqld.exe") `
    -ArgumentList "--defaults-file=`"$myIni`"", "--console" `
    -WindowStyle Hidden `
    -PassThru

Start-Sleep -Seconds 3
</#if>

$pidFile = Join-Path $pidDir "mysql.pid"
$process.Id | Out-File -FilePath $pidFile -Encoding UTF8
Write-Host "  PID: $($process.Id) -> $pidFile" -ForegroundColor DarkGray
