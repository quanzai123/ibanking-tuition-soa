#!/bin/bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

echo "========================================================================"
echo "🐳 KHỞI CHẠY IBANKING TUITION MICROSERVICES VỚI DOCKER COMPOSE"
echo "========================================================================"

# 1. Đóng gói JAR nếu chưa có
if [ ! -f "gateway-service/target/gateway-service-1.0.0.jar" ]; then
    echo "📦 Đang biên dịch mã nguồn và đóng gói JAR các microservices..."
    ./mvnw clean package -DskipTests
fi

# 2. Khởi chạy Docker Compose
echo "🚀 Đang kích hoạt các container Docker..."
docker compose up --build -d

echo ""
echo "========================================================================"
echo "🎉 HỆ THỐNG DOCKER ĐÃ SẴN SÀNG!"
echo "🌐 Giao diện Web iBanking: http://localhost:8080"
echo "📬 Hộp thư Webmail MailHog: http://localhost:8025"
echo "========================================================================"
echo "👉 Xem logs: docker compose logs -f"
echo "👉 Dừng hệ thống: docker compose down"
