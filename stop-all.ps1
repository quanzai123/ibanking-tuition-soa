# ==============================================================================
# SCRIPT DỪNG HỆ THỐNG IBANKING TUITION DÀNH CHO WINDOWS POWERSHELL
# ==============================================================================
$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Set-Location $PSScriptRoot

Write-Host "🛑 Đang tắt toàn bộ 5 Microservices trên ports 8080 - 8084..." -ForegroundColor Yellow

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

Write-Host "✅ Toàn bộ hệ thống đã tắt an toàn!" -ForegroundColor Green
Read-Host "Nhấn Enter để đóng..."
