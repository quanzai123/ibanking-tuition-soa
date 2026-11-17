@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ========================================================================
echo 🐳 KHỞI CHẠY IBANKING TUITION MICROSERVICES VỚI DOCKER COMPOSE
echo ========================================================================

if not exist "gateway-service\target\gateway-service-1.0.0.jar" (
    echo 📦 Đang biên dịch mã nguồn và đóng gói JAR các microservices...
    call mvnw.cmd clean package -DskipTests
)

echo 🚀 Đang kích hoạt các container Docker...
docker compose up --build -d

echo.
echo ========================================================================
echo 🎉 HỆ THỐNG DOCKER ĐÃ SẴN SÀNG!
echo 🌐 Giao diện Web iBanking: http://localhost:8080
echo 📬 Hộp thư Webmail MailHog: http://localhost:8025
echo ========================================================================
echo 👉 Xem logs: docker compose logs -f
echo 👉 Dừng hệ thống: docker compose down
pause
