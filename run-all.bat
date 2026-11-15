@echo off
chcp 65001 >nul
echo ========================================================================
echo 🏦 KHỞI ĐỘNG HỆ THỐNG IBANKING TUITION MICROSERVICES (SOA CS504070 - TDTU)
echo ========================================================================

:: 1. Dọn dẹp các cổng nếu đang chạy
echo 🧹 Bước 1: Dọn dẹp các cổng mạng (8080 - 8084)...
for %%p in (8080 8081 8082 8083 8084) do (
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":%%p" ^| findstr "LISTENING"') do (
        taskkill /F /PID %%a >nul 2>&1
    )
)

:: 2. Build dự án nếu chưa có JAR
if not exist "gateway-service\target\gateway-service-1.0.0.jar" (
    echo 📦 Bước 2: Đang biên dịch và đóng gói JAR cho toàn bộ Microservices...
    call mvnw.cmd clean package -DskipTests
) else (
    echo ✅ Đã tìm thấy các tệp JAR đóng gói sẵn!
)

if not exist logs mkdir logs

echo 🚀 Bước 3: Đang khởi chạy lần lượt 5 Microservices...

echo    [1/5] Khởi động Account ^& Auth Service (:8081)...
start "Account Service" /B java -jar account-service\target\account-service-1.0.0.jar > logs\account-service.log 2>&1

echo    [2/5] Khởi động Tuition Service TDTU (:8083)...
start "Tuition Service" /B java -jar tuition-service\target\tuition-service-1.0.0.jar > logs\tuition-service.log 2>&1

echo    [3/5] Khởi động OTP ^& Notification Service (:8084)...
start "OTP Service" /B java -jar otp-service\target\otp-service-1.0.0.jar > logs\otp-service.log 2>&1

timeout /t 3 /nobreak >nul

echo    [4/5] Khởi động Payment Saga Orchestrator Service (:8082)...
start "Payment Service" /B java -jar payment-service\target\payment-service-1.0.0.jar > logs\payment-service.log 2>&1

timeout /t 3 /nobreak >nul

echo    [5/5] Khởi động API Gateway ^& Web Frontend (:8080)...
start "Gateway Service" /B java -jar gateway-service\target\gateway-service-1.0.0.jar > logs\gateway-service.log 2>&1

timeout /t 4 /nobreak >nul

echo ========================================================================
echo 🎉 HỆ THỐNG ĐÃ SẴN SÀNG HOẠT ĐỘNG!
echo 🌐 TRUY CẬP GIAO DIỆN WEB NGAY TẠI: http://localhost:8080
echo ========================================================================
echo Danh sách các cổng dịch vụ nội bộ:
echo    - [API Gateway ^& UI]      : http://localhost:8080
echo    - [Account ^& Auth Service]: http://localhost:8081
echo    - [Payment Saga Service]  : http://localhost:8082
echo    - [Tuition Service TDTU]  : http://localhost:8083
echo    - [OTP ^& Mail Service]    : http://localhost:8084
echo.
echo Tài khoản mẫu để đăng nhập và chấm điểm:
echo    1) user1   / password123 (Số dư: 35.000.000 VND)
echo    2) user2   / password123 (Số dư: 10.000.000 VND)
echo    3) parent1 / password123 (Số dư: 50.000.000 VND)
echo.
echo MSSV mẫu tra cứu học phí:
echo    • 524H0122 (Nguyễn Anh Quân - Học phí:  9.500.000 VND - CHƯA ĐÓNG)
echo    • 524H0123 (Nguyễn Hồng Quân - Học phí: 8.200.000 VND - CHƯA ĐÓNG)
echo    • 524H0888 (Phạm Minh Đức    - Học phí: 7.800.000 VND - ĐÃ THANH TOÁN)
echo.
echo 👉 Để tắt toàn bộ hệ thống: chạy file stop-all.bat
echo ========================================================================
pause
