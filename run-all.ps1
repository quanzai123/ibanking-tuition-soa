# ==============================================================================
# SCRIPT KHỞI ĐỘNG HỆ THỐNG IBANKING TUITION DÀNH CHO WINDOWS POWERSHELL
# ==============================================================================
$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Set-Location $PSScriptRoot

Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "🏦 KHỞI ĐỘNG HỆ THỐNG IBANKING TUITION MICROSERVICES (SOA CS504070 - TDTU)" -ForegroundColor Cyan
Write-Host "========================================================================" -ForegroundColor Cyan

# 1. Kiểm tra môi trường Java
Write-Host "☕ [1/4] Kiểm tra môi trường Java..." -ForegroundColor Yellow
try {
    $javaVer = java -version 2>&1
    Write-Host "   -> Đã tìm thấy Java!" -ForegroundColor Green
} catch {
    Write-Host "❌ [LỖI] Không tìm thấy Java trên máy tính! Vui lòng cài đặt Java 21 (JDK 21)." -ForegroundColor Red
    Read-Host "Nhấn Enter để thoát..."
    exit 1
}

# 2. Dọn dẹp các cổng mạng 8080 - 8084
Write-Host "🧹 [2/4] Dọn dẹp các cổng mạng 8080 - 8084..." -ForegroundColor Yellow
8080, 8081, 8082, 8083, 8084 | ForEach-Object {
    $port = $_
    $conn = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($conn) {
        $conn.OwningProcess | Select-Object -Unique | ForEach-Object {
            Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue
            Write-Host "   -> Đã tắt tiến trình PID $_ trên cổng $port" -ForegroundColor Gray
        }
    }
}

# 3. Build dự án nếu chưa có JAR
if (-not (Test-Path "gateway-service\target\gateway-service-1.0.0.jar")) {
    Write-Host "📦 [3/4] Đang biên dịch mã nguồn và đóng gói JAR các Microservices..." -ForegroundColor Yellow
    if (Test-Path "mvnw.cmd") {
        .\mvnw.cmd clean package -DskipTests
    } else {
        mvn clean package -DskipTests
    }
} else {
    Write-Host "✅ [3/4] Đã tìm thấy các tệp JAR đóng gói sẵn!" -ForegroundColor Green
}

if (-not (Test-Path "gateway-service\target\gateway-service-1.0.0.jar")) {
    Write-Host "❌ [LỖI] Không thể đóng gói JAR! Vui lòng kiểm tra lại kết nối mạng." -ForegroundColor Red
    Read-Host "Nhấn Enter để thoát..."
    exit 1
}

if (-not (Test-Path "logs")) { New-Item -ItemType Directory -Path "logs" | Out-Null }

# 4. Khởi chạy 5 Microservices
Write-Host "🚀 [4/4] Đang khởi chạy lần lượt 5 Microservices..." -ForegroundColor Yellow

Write-Host "   [1/5] Khởi động Account & Auth Service (:8081)..." -ForegroundColor Gray
Start-Process -FilePath "java" -ArgumentList "-jar account-service\target\account-service-1.0.0.jar" -RedirectStandardOutput "logs\account-service.log" -RedirectStandardError "logs\account-service.err.log" -WindowStyle Hidden

Write-Host "   [2/5] Khởi động Tuition Service TDTU (:8083)..." -ForegroundColor Gray
Start-Process -FilePath "java" -ArgumentList "-jar tuition-service\target\tuition-service-1.0.0.jar" -RedirectStandardOutput "logs\tuition-service.log" -RedirectStandardError "logs\tuition-service.err.log" -WindowStyle Hidden

Write-Host "   [3/5] Khởi động OTP & Notification Service (:8084)..." -ForegroundColor Gray
Start-Process -FilePath "java" -ArgumentList "-jar otp-service\target\otp-service-1.0.0.jar" -RedirectStandardOutput "logs\otp-service.log" -RedirectStandardError "logs\otp-service.err.log" -WindowStyle Hidden

Start-Sleep -Seconds 3

Write-Host "   [4/5] Khởi động Payment Saga Orchestrator Service (:8082)..." -ForegroundColor Gray
Start-Process -FilePath "java" -ArgumentList "-jar payment-service\target\payment-service-1.0.0.jar" -RedirectStandardOutput "logs\payment-service.log" -RedirectStandardError "logs\payment-service.err.log" -WindowStyle Hidden

Start-Sleep -Seconds 3

Write-Host "   [5/5] Khởi động API Gateway & Web Frontend (:8080)..." -ForegroundColor Gray
Start-Process -FilePath "java" -ArgumentList "-jar gateway-service\target\gateway-service-1.0.0.jar" -RedirectStandardOutput "logs\gateway-service.log" -RedirectStandardError "logs\gateway-service.err.log" -WindowStyle Hidden

Start-Sleep -Seconds 4

Write-Host "========================================================================" -ForegroundColor Green
Write-Host "🎉 HỆ THỐNG ĐÃ SẴN SÀNG HOẠT ĐỘNG!" -ForegroundColor Green
Write-Host "🌐 TRUY CẬP GIAO DIỆN WEB NGAY TẠI: http://localhost:8080" -ForegroundColor White
Write-Host "========================================================================" -ForegroundColor Green
Write-Host "Danh sách các cổng dịch vụ nội bộ:" -ForegroundColor Cyan
Write-Host "   - [API Gateway & UI]      : http://localhost:8080"
Write-Host "   - [Account & Auth Service]: http://localhost:8081"
Write-Host "   - [Payment Saga Service]  : http://localhost:8082"
Write-Host "   - [Tuition Service TDTU]  : http://localhost:8083"
Write-Host "   - [OTP & Mail Service]    : http://localhost:8084"
Write-Host ""
Write-Host "Tài khoản mẫu để đăng nhập: user1 / password123 (Số dư: 35.000.000 VND)"
Write-Host "MSSV mẫu tra cứu:          524H0122 (Nguyễn Anh Quân - 9.500.000 VND - CHƯA ĐÓNG)"
Write-Host ""
Write-Host "👉 Để dừng hệ thống: Chạy file .\stop-all.ps1 hoặc stop-all.bat" -ForegroundColor Yellow
Write-Host "========================================================================" -ForegroundColor Green
Read-Host "Nhấn Enter để giữ cửa sổ..."
