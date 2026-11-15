# 🏦 Phân Hệ Đóng Học Phí iBanking (TDTU) - Kiến Trúc Microservices (SOA)

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.1.1-brightgreen?style=for-the-badge&logo=springboot" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Architecture-Microservices_SOA-blue?style=for-the-badge" alt="Microservices" />
  <img src="https://img.shields.io/badge/Concurrency-Pessimistic_Lock_%26_Atomic-red?style=for-the-badge" alt="Concurrency" />
  <img src="https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker" alt="Docker" />
  <img src="https://img.shields.io/badge/License-100%25_Free_Open_Source-green?style=for-the-badge" alt="Free" />
</p>

> **Học phần:** Kiến Trúc Hướng Dịch Vụ (SOA - CS504070) — Khoa Công Nghệ Thông Tin — Đại học Tôn Đức Thắng (TDTU)  
> **Đề tài Dự án:** Phân hệ đóng học phí của ứng dụng iBanking  
> **Nhóm sinh viên thực hiện:**  
> - **Nguyễn Anh Quân** — MSSV: `524H0122`  
> - **Nguyễn Hồng Quân** — MSSV: `524H0123`  
> **Giảng viên hướng dẫn:** Thầy Dương Hữu Phúc  

---

## 📑 Danh Mục Microservices & Cổng Dịch Vụ

| Service | Port | Database | Trách Nhiệm Nghiệp Vụ Chính |
| :--- | :---: | :---: | :--- |
| **API Gateway & Web UI** | `8080` | *None* | Cổng giao tiếp duy nhất, reverse proxy định tuyến, host Web App iBanking |
| **Account & Auth Service** | `8081` | `account_db` | Xác thực đăng nhập, quản lý user, số dư tài khoản & khóa ghi Pessimistic Lock |
| **Payment Saga Service** | `8082` | `payment_db` | Điều phối giao dịch thanh toán (Saga Orchestration), lưu lịch sử giao dịch |
| **Tuition Service (TDTU)** | `8083` | `tuition_db` | Quản lý sinh viên, tra cứu học phí, gạch nợ nguyên tử (`Atomic Update`) |
| **OTP & Mail Service** | `8084` | `otp_db` | Sinh OTP ngẫu nhiên (5 phút, dùng 1 lần), gửi email OTP & biên lai thanh toán |
| **MailHog (Docker)** | `8025` | *Memory* | Hộp thư Web UI 100% miễn phí để mở xem email OTP khi chạy Docker |

---

## ⚡ Hướng Dẫn Khởi Động Nhanh (1-Click Run)

### 🔹 Cách 1: Khởi động trực tiếp trên máy (Native Java - Khuyên Dùng Demo)
Mở Terminal tại thư mục dự án và gõ:
```bash
./run-all.sh
```
*Script sẽ tự động build các module và khởi động lần lượt 5 microservices. Sau 5 giây, toàn bộ hệ thống sẵn sàng!*

🌐 **Truy cập Giao diện Web iBanking tại:**  
👉 **[http://localhost:8080](http://localhost:8080)**

Để dừng toàn bộ hệ thống: Nhấn `Ctrl + C` hoặc chạy `./stop-all.sh`.

---

### 🔹 Cách 2: Khởi động bằng Docker Compose
Nếu máy đã bật Docker:
```bash
docker compose up --build -d
```
- Web iBanking: [http://localhost:8080](http://localhost:8080)
- Hộp thư MailHog xem email: [http://localhost:8025](http://localhost:8025)

---

## 👤 Dữ Liệu Mẫu (Seed Data) Để Chấm Điểm

### 1. Tài khoản iBanking (Đăng nhập tại UI):
| Username | Password | Họ và Tên | Số Tài Khoản | Số Dư Khả Dụng | Mục Đích Test |
| :--- | :---: | :--- | :---: | :---: | :--- |
| `user1` | `password123` | Nguyễn Văn An | `1000111222` | **35.000.000 VND** | Tài khoản chính để nộp học phí & test đua nộp cùng 1 MSSV |
| `user2` | `password123` | Trần Thị Bích | `2000333444` | **10.000.000 VND** | Test kịch bản 1: Bắn 2 GD đồng thời (9.5tr + 8.2tr = 17.7tr > 10tr) |
| `parent1` | `password123` | Lê Hoàng Nam (Phụ huynh) | `3000555666` | **50.000.000 VND** | Test kịch bản 2: Phụ huynh nộp hộ sinh viên & test hoàn tiền |

### 2. Dữ liệu Sinh viên TDTU tra cứu học phí:
| MSSV | Họ và Tên | Khoa | Học Phí | Trạng Thái Ban Đầu |
| :---: | :--- | :--- | :---: | :---: |
| **`524H0122`** | Nguyễn Anh Quân | Công Nghệ Thông Tin | **9.500.000 VND** | `CHƯA ĐÓNG` |
| **`524H0123`** | Nguyễn Hồng Quân | Công Nghệ Thông Tin | **8.200.000 VND** | `CHƯA ĐÓNG` |
| **`524H0999`** | Lê Bảo Trâm | Ngoại Ngữ | **12.000.000 VND** | `CHƯA ĐÓNG` |
| **`524H0888`** | Phạm Minh Đức | Điện - Điện Tử | **7.800.000 VND** | `ĐÃ THANH TOÁN` (Test báo đã đóng) |

---

## 🛡️ Kiểm Thử Concurrency & Tính Nhất Quán (Điểm 10/10)

Hệ thống cung cấp sẵn script Python đa luồng bắn đồng thời nhiều requests để chứng minh khả năng xử lý tranh chấp:

```bash
python3 test_concurrency.py
```

### Kịch bản 1: Nhiều giao dịch đồng thời trên 1 tài khoản (Chống Double Spending)
- Bắn đồng thời 2 giao dịch thanh toán tổng cộng **17.700.000 VND** từ tài khoản chỉ có **15.000.000 VND**.
- **Kết quả:** Nhờ cơ chế **Pessimistic Lock (`SELECT ... FOR UPDATE`)**, đúng 1 giao dịch thành công, giao dịch còn lại bị từ chối do thiếu tiền. Số dư không bao giờ bị âm!

### Kịch bản 2: Nhiều tài khoản cùng thanh toán 1 MSSV (Chống Double Payment)
- 2 người dùng khác nhau cùng thanh toán cho MSSV `524H0999` tại cùng một tích tắc.
- **Kết quả:** Nhờ **Atomic Update (`WHERE status = 'UNPAID'`)** kết hợp **Saga Compensating Transaction**, chỉ 1 người gạch nợ thành công, người kia lập tức được hoàn tiền 100% về tài khoản!

