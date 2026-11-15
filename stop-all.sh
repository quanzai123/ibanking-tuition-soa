#!/bin/bash

echo "🛑 Đang tắt các tiến trình Microservices trên các port 8080 - 8084..."

for port in 8080 8081 8082 8083 8084; do
    pid=$(lsof -ti tcp:$port)
    if [ ! -z "$pid" ]; then
        echo "   -> Đang tắt tiến trình PID $pid trên port $port"
        kill -9 $pid 2>/dev/null
    fi
done

echo "✅ Đã tắt toàn bộ các services!"

