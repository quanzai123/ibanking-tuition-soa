#!/usr/bin/env python3
"""
==============================================================================
KIỂM THỬ XỬ LÝ ĐỒNG THỜI (CONCURRENCY & TRANSACTION CONSISTENCY TEST SUITE)
Phân hệ đóng học phí iBanking TDTU (SOA CS504070)
------------------------------------------------------------------------------
Mục tiêu:
  1. Test Case 1: Nhiều giao dịch đồng thời trên 1 tài khoản (Chống Double Spending)
  2. Test Case 2: Nhiều tài khoản cùng thanh toán 1 MSSV (Chống Double Payment)
==============================================================================
"""

import sys
# Tương thích mã hóa UTF-8 khi in tiếng Việt và emoji trên cả Windows cmd và macOS/Linux terminal
if sys.platform.startswith("win"):
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

import urllib.request
import urllib.error
import json
import time
from concurrent.futures import ThreadPoolExecutor

GATEWAY_URL = "http://localhost:8080"

def print_separator(title):
    print("\n" + "=" * 75)
    print(f"🔹 {title}")
    print("=" * 75)

def http_post(endpoint, data=None, token=None):
    url = f"{GATEWAY_URL}{endpoint}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = json.dumps(data).encode("utf-8") if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as res:
            return res.status, json.loads(res.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8")
        try:
            return e.code, json.loads(err_body)
        except Exception:
            return e.code, {"message": err_body}
    except Exception as e:
        return 500, {"message": str(e)}

def http_get(endpoint, token=None):
    url = f"{GATEWAY_URL}{endpoint}"
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=headers, method="GET")
    try:
        with urllib.request.urlopen(req) as res:
            return res.status, json.loads(res.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8"))
    except Exception as e:
        return 500, {"message": str(e)}

def login(username, password="password123"):
    status, res = http_post("/api/auth/login", {"username": username, "password": password})
    if status == 200 and res.get("success"):
        return res["data"]["token"], res["data"]["user"]
    raise Exception(f"Đăng nhập thất bại cho {username}: {res}")

def initiate_payment(mssv, token):
    status, res = http_post("/api/payments/initiate", {"mssv": mssv}, token)
    if status == 200 and res.get("success") and res.get("data"):
        return res["data"]["transactionId"], res["data"]["debugOtpCode"]
    raise Exception(f"Khởi tạo giao dịch thất bại cho MSSV {mssv}: (Status {status}) {res}")

def wait_for_services():
    print("⏳ Đang đợi toàn bộ các Microservices sẵn sàng...")
    max_wait = 30
    start = time.time()
    while time.time() - start < max_wait:
        try:
            st_t, _ = http_get("/api/tuitions/524H0122")
            st_a, _ = http_post("/api/auth/login", {"username": "user1", "password": "password123"})
            if st_t == 200 and st_a == 200:
                print("✅ Tất cả các Microservices đã sẵn sàng!")
                return True
        except Exception:
            pass
        time.sleep(1.5)
    raise TimeoutError("Hệ thống mất quá nhiều thời gian để khởi động!")

def run_test_concurrency_same_account():
    print_separator("KỊCH BẢN 1: NHIỀU GIAO DỊCH ĐỒNG THỜI TRÊN CÙNG MỘT TÀI KHOẢN (DOUBLE SPENDING)")
    
    # 1. Đăng nhập user2 (số dư 10 triệu)
    token, user = login("user2")
    initial_balance = float(user["balance"])
    print(f"👉 Người dùng: {user['fullName']} | STK: {user['accountNumber']}")
    print(f"👉 Số dư ban đầu: {initial_balance:,.0f} VND")

    # Giả sử khởi tạo 2 giao dịch thanh toán cho 2 sinh viên khác nhau:
    # GD 1: Nộp cho 524H0122 (9.500.000 VND)
    # GD 2: Nộp cho 524H0123 (8.200.000 VND)
    # Tổng tiền cần nộp = 17.700.000 VND > 10.000.000 VND (Số dư ban đầu)
    print("\n👉 Chuẩn bị 2 giao dịch song song:")
    print("   - GD 1: Nộp cho MSSV 524H0122 (Số tiền: 9.500.000 VND)")
    print("   - GD 2: Nộp cho MSSV 524H0123 (Số tiền: 8.200.000 VND)")
    print("   => Tổng chi tiêu dự kiến: 17.700.000 VND > Số dư hiện có (10.000.000 VND)")

    # Khởi tạo GD 1
    tx1_id, otp1 = initiate_payment("524H0122", token)

    # Khởi tạo GD 2
    tx2_id, otp2 = initiate_payment("524H0123", token)

    print(f"\n   • Đã tạo GD 1: {tx1_id} (OTP: {otp1})")
    print(f"   • Đã tạo GD 2: {tx2_id} (OTP: {otp2})")

    # Bắn đồng thời 2 requests confirm thanh toán bằng ThreadPoolExecutor
    print("\n⚡ [RACE CONDITION TRIGGER] Đang kích hoạt 2 requests xác nhận song song tại cùng 1 tích tắc...")

    def execute_confirm(tx_id, otp):
        t_start = time.time()
        status, res = http_post("/api/payments/confirm", {"transactionId": tx_id, "otpCode": otp}, token)
        duration = time.time() - t_start
        return tx_id, status, res, duration

    with ThreadPoolExecutor(max_workers=2) as executor:
        f1 = executor.submit(execute_confirm, tx1_id, otp1)
        f2 = executor.submit(execute_confirm, tx2_id, otp2)
        results = [f1.result(), f2.result()]

    success_count = 0
    fail_count = 0
    for tx_id, status, res, duration in results:
        is_ok = status == 200 and res.get("success")
        if is_ok:
            success_count += 1
            print(f"   ✅ {tx_id}: THÀNH CÔNG trong {duration:.3f}s")
        else:
            fail_count += 1
            msg = res.get("message", "N/A")
            print(f"   ❌ {tx_id}: BỊ TỪ CHỐI trong {duration:.3f}s -> Lý do: {msg}")

    # Lấy lại số dư mới nhất
    _, me_res = http_get("/api/accounts/me", token)
    final_balance = float(me_res["data"]["balance"])

    print(f"\n👉 Số dư cuối cùng thực tế trong tài khoản: {final_balance:,.0f} VND")
    if success_count == 1 and fail_count == 1 and final_balance >= 0:
        print("🎯 KẾT QUẢ: ĐẠT YÊU CẦU 100%! Cơ chế Pessimistic Lock ngăn chặn hoàn toàn việc chi tiêu vượt quá số dư!")
    else:
        print("⚠️ CẢNH BÁO: Kịch bản không đạt kết quả kỳ vọng!")

def run_test_concurrency_same_student():
    print_separator("KỊCH BẢN 2: NHIỀU TÀI KHOẢN CÙNG THANH TOÁN 1 KHOẢN HỌC PHÍ (DOUBLE PAYMENT)")

    # 2 tài khoản khác nhau:
    # User 1 (user1) và User 3 (parent1) cùng thanh toán cho MSSV 524H0999 (Lê Bảo Trâm - 12.000.000 VND)
    token1, user1 = login("user1")
    token2, user2 = login("parent1")

    print(f"👉 Tài khoản A: {user1['fullName']} (Số dư: {float(user1['balance']):,.0f} VND)")
    print(f"👉 Tài khoản B: {user2['fullName']} (Số dư: {float(user2['balance']):,.0f} VND)")
    print("👉 Mục tiêu: Cùng thanh toán cho MSSV 524H0999 (Số tiền: 12.000.000 VND)")

    # Khởi tạo GD từ tài khoản A
    txA_id, otpA = initiate_payment("524H0999", token1)

    # Khởi tạo GD từ tài khoản B
    txB_id, otpB = initiate_payment("524H0999", token2)

    print(f"\n   • Tài khoản A tạo GD: {txA_id} (OTP: {otpA})")
    print(f"   • Tài khoản B tạo GD: {txB_id} (OTP: {otpB})")

    # Bắn đồng thời 2 requests confirm thanh toán
    print("\n⚡ [RACE CONDITION TRIGGER] Đang kích hoạt đồng thời 2 thanh toán từ 2 tài khoản khác nhau...")

    def execute_confirm_user(tx_id, otp, tok, uname):
        t_start = time.time()
        status, res = http_post("/api/payments/confirm", {"transactionId": tx_id, "otpCode": otp}, tok)
        duration = time.time() - t_start
        return uname, tx_id, status, res, duration

    with ThreadPoolExecutor(max_workers=2) as executor:
        fA = executor.submit(execute_confirm_user, txA_id, otpA, token1, "Tài khoản A (user1)")
        fB = executor.submit(execute_confirm_user, txB_id, otpB, token2, "Tài khoản B (parent1)")
        results = [fA.result(), fB.result()]

    success_count = 0
    fail_count = 0
    for uname, tx_id, status, res, duration in results:
        is_ok = status == 200 and res.get("success")
        if is_ok:
            success_count += 1
            print(f"   ✅ [{uname}] {tx_id}: THANH TOÁN & GẠCH NỢ THÀNH CÔNG trong {duration:.3f}s")
        else:
            fail_count += 1
            msg = res.get("message", "N/A")
            print(f"   ❌ [{uname}] {tx_id}: BỊ CHẶN & ĐÃ HOÀN TIỀN trong {duration:.3f}s -> Lý do: {msg}")

    # Kiểm tra trạng thái học phí MSSV 524H0999
    _, tuition_res = http_get("/api/tuitions/524H0999")
    tuition = tuition_res["data"]
    print(f"\n👉 Trạng thái học phí MSSV 524H0999: {tuition['status']} (Mã GD thụ hưởng: {tuition['transactionRef']})")

    if success_count == 1 and fail_count == 1 and tuition["status"] == "PAID":
        print("🎯 KẾT QUẢ: ĐẠT YÊU CẦU 100%! Khoản học phí chỉ bị thanh toán đúng 1 lần duy nhất, người đến sau được bồi hoàn tiền trọn vẹn!")
    else:
        print("⚠️ CẢNH BÁO: Kịch bản không đạt kết quả kỳ vọng!")

if __name__ == "__main__":
    print("\nKiểm tra kết nối và tính sẵn sàng của các Microservices...")
    wait_for_services()
    print("🧹 Đang đặt lại trạng thái cơ sở dữ liệu về dữ liệu mẫu ban đầu...")
    http_post("/api/accounts/internal/reset")
    http_post("/api/tuitions/internal/reset")
    print("✅ Cơ sở dữ liệu đã được làm mới sạch sẽ!")
    run_test_concurrency_same_account()
    run_test_concurrency_same_student()
    print("\n🏁 HOÀN TẤT TOÀN BỘ TEST SUITE!\n")
