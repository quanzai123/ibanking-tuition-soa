@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo 🛑 Đang tắt toàn bộ 5 Microservices (Ports 8080 - 8084)...
for %%p in (8080 8081 8082 8083 8084) do (
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":%%p" ^| findstr "LISTENING"') do (
        taskkill /F /PID %%a >nul 2>&1
    )
)
echo ✅ Toàn bộ hệ thống đã tắt an toàn!
pause
