#!/bin/bash

# ==============================================================================
# SCRIPT KHỞI ĐỘNG HỆ THỐNG PHÂN HỆ ĐÓNG HỌC PHÍ IBANKING TDTU (SOA MICROSERVICES)
# ==============================================================================

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

echo "========================================================================"
echo "🏦 KHỞI ĐỘNG HỆ THỐNG IBANKING TUITION MICROSERVICES (SOA CS504070 - TDTU)"
echo "========================================================================"

# 1. Dọn dẹp các tiến trình cũ trên ports 8080 - 8084
echo "🧹 Bước 1: Dọn dẹp các cổng mạng..."
for port in 8080 8081 8082 8083 8084; do
    pid=$(lsof -ti tcp:$port 2>/dev/null || true)
    if [ ! -z "$pid" ]; then
        echo "   - Tắt tiến trình cũ PID $pid tại port $port"
        kill -9 $pid 2>/dev/null || true
    fi
done

# 2. Build dự án nếu chưa có JAR
if [ ! -f "gateway-service/target/gateway-service-1.0.0.jar" ]; then
    echo "📦 Bước 2: Đang biên dịch và đóng gói JAR cho toàn bộ Microservices..."
    ./mvnw clean package -DskipTests
else
    echo "✅ Đã tìm thấy các tệp JAR đóng gói sẵn!"
fi

mkdir -p logs

echo "🚀 Bước 3: Đang khởi chạy lần lượt 5 Microservices..."

# Service 1: Account Service (Port 8081)
echo "   [1/5] Khởi động Account & Auth Service (:8081)..."
java -jar account-service/target/account-service-1.0.0.jar > logs/account-service.log 2>&1 &
PID_ACCOUNT=$!

# Service 2: Tuition Service (Port 8083)
echo "   [2/5] Khởi động Tuition Service TDTU (:8083)..."
java -jar tuition-service/target/tuition-service-1.0.0.jar > logs/tuition-service.log 2>&1 &
PID_TUITION=$!

# Service 3: OTP & Notification Service (Port 8084)
echo "   [3/5] Khởi động OTP & Notification Service (:8084)..."
java -jar otp-service/target/otp-service-1.0.0.jar > logs/otp-service.log 2>&1 &
PID_OTP=$!

sleep 3

# Service 4: Payment Orchestrator Service (Port 8082)
echo "   [4/5] Khởi động Payment Saga Orchestrator Service (:8082)..."
java -jar payment-service/target/payment-service-1.0.0.jar > logs/payment-service.log 2>&1 &
PID_PAYMENT=$!

sleep 3

# Service 5: API Gateway & Frontend (Port 8080)
echo "   [5/5] Khởi động API Gateway & Web Frontend (:8080)..."
java -jar gateway-service/target/gateway-service-1.0.0.jar > logs/gateway-service.log 2>&1 &
PID_GATEWAY=$!

cleanup() {
    echo ""
    echo "🛑 Đang dừng toàn bộ 5 Microservices..."
    kill $PID_ACCOUNT $PID_TUITION $PID_OTP $PID_PAYMENT $PID_GATEWAY 2>/dev/null || true
    echo "✅ Toàn bộ hệ thống đã tắt an toàn!"
    exit 0
}

trap cleanup SIGINT SIGTERM

echo "⏳ Đang đợi các services hoàn tất khởi tạo..."
sleep 4

echo "========================================================================"
echo "🎉 HỆ THỐNG ĐÃ SẴN SÀNG HOẠT ĐỘNG!"
echo "🌐 TRUY CẬP GIAO DIỆN WEB NGAY TẠI: http://localhost:8080"
echo "========================================================================"
echo "Danh sách các cổng dịch vụ nội bộ:"
echo "   - [API Gateway & UI]      : http://localhost:8080"
echo "   - [Account & Auth Service]: http://localhost:8081"
echo "   - [Payment Saga Service]  : http://localhost:8082"
echo "   - [Tuition Service TDTU]  : http://localhost:8083"
echo "   - [OTP & Mail Service]    : http://localhost:8084"
echo ""
echo "Tài khoản mẫu để đăng nhập và chấm điểm:"
echo "   1) user1   / password123 (Số dư: 35.000.000 VND - nộp học phí & test kịch bản 2)"
echo "   2) user2   / password123 (Số dư: 10.000.000 VND - test kịch bản 1 Concurrency)"
echo "   3) parent1 / password123 (Số dư: 50.000.000 VND - test phụ huynh nộp hộ & hoàn tiền)"
echo ""
echo "MSSV mẫu tra cứu học phí:"
echo "   • 524H0122 (Nguyễn Anh Quân - Học phí:  9.500.000 VND - CHƯA ĐÓNG)"
echo "   • 524H0123 (Nguyễn Hồng Quân - Học phí: 8.200.000 VND - CHƯA ĐÓNG)"
echo "   • 524H0888 (Phạm Minh Đức    - Học phí: 7.800.000 VND - ĐÃ THANH TOÁN)"
echo ""
echo "👉 Nhấn Ctrl + C để dừng toàn bộ hệ thống bất cứ lúc nào."
echo "========================================================================"

wait

