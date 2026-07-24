@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start_services.ps1"
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Start script failed with code %errorlevel%
    echo Check messages above. Press any key to exit...
    pause > nul
)