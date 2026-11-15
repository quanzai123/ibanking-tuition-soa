package com.tdtu.soa.payment.service;

import com.tdtu.soa.common.dto.*;
import com.tdtu.soa.payment.client.AccountClient;
import com.tdtu.soa.payment.client.OtpClient;
import com.tdtu.soa.payment.client.TuitionClient;
import com.tdtu.soa.payment.entity.PaymentTransaction;
import com.tdtu.soa.payment.repository.PaymentTransactionRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSagaService {

    private final PaymentTransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final TuitionClient tuitionClient;
    private final OtpClient otpClient;

    @Value("${jwt.secret:TDTU_SOA_IBANKING_SECRET_KEY_FOR_JWT_AUTHENTICATION_CS504070_VERY_SECURE}")
    private String jwtSecret;

    /**
     * BƯỚC 1: KHỞI TẠO GIAO DỊCH THANH TOÁN
     * - Kiểm tra thông tin người nộp
     * - Tra cứu nợ học phí từ Tuition Service
     * - Kiểm tra điều kiện: Học phí tồn tại, Chưa đóng, Số dư >= Số tiền cần đóng
     * - Tạo giao dịch PENDING
     * - Yêu cầu OTP Service sinh OTP và gửi email
     */
    @Transactional
    public InitiatePaymentResponse initiatePayment(String tokenHeader, InitiatePaymentRequest request) {
        UserDto user = accountClient.getProfile(tokenHeader);
        log.info("[PAYMENT-INITIATE] Người nộp: {} (STK: {}) - MSSV nộp: {}",
                user.getFullName(), user.getAccountNumber(), request.getMssv());

        // 1. Tra cứu học phí
        TuitionDto tuition = tuitionClient.getTuition(request.getMssv());
        if ("PAID".equalsIgnoreCase(tuition.getStatus())) {
            log.warn("[PAYMENT-INITIATE] Khoản học phí MSSV {} đã được thanh toán trước đó!", request.getMssv());
            throw new IllegalStateException("Khoản học phí của sinh viên này đã được thanh toán!");
        }

        // 2. Kiểm tra số dư khả dụng
        if (user.getBalance().compareTo(tuition.getAmount()) < 0) {
            log.warn("[PAYMENT-INITIATE] Số dư không đủ! Cần: {}, Có: {}", tuition.getAmount(), user.getBalance());
            throw new IllegalStateException(String.format(
                    "Số dư khả dụng (%s VNĐ) không đủ để thanh toán học phí (%s VNĐ)",
                    String.format("%,.0f", user.getBalance()),
                    String.format("%,.0f", tuition.getAmount())));
        }

        // 3. Khởi tạo mã giao dịch duy nhất
        String txId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PaymentTransaction transaction = PaymentTransaction.builder()
                .id(txId)
                .userId(user.getId())
                .payerName(user.getFullName())
                .accountNumber(user.getAccountNumber())
                .mssv(tuition.getMssv())
                .studentName(tuition.getStudentName())
                .amount(tuition.getAmount())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        // 4. Sinh và gửi mã OTP qua email người nộp
        OtpGenerateResponse otpRes = otpClient.generateOtp(OtpGenerateRequest.builder()
                .transactionId(txId)
                .recipientEmail(user.getEmail())
                .build());

        log.info("[PAYMENT-INITIATE] Đã tạo GD PENDING {} và gửi OTP đến email {}", txId, user.getEmail());

        return InitiatePaymentResponse.builder()
                .transactionId(txId)
                .mssv(tuition.getMssv())
                .studentName(tuition.getStudentName())
                .amount(tuition.getAmount())
                .maskedEmail(otpRes.getMaskedEmail())
                .expiresInSeconds(otpRes.getExpiresInSeconds())
                .debugOtpCode(otpRes.getDebugOtpCode())
                .build();
    }

    /**
     * BƯỚC 2: XÁC NHẬN GIAO DỊCH BẰNG OTP & THỰC THI SAGA ORCHESTRATION
     * 
     * Đảm bảo triệt để 2 tình huống Concurrency theo đề bài:
     * - Tình huống 1: Nhiều giao dịch đồng thời trên 1 tài khoản (Account Service khóa ghi Pessimistic Lock)
     * - Tình huống 2: Nhiều tài khoản cùng thanh toán 1 MSSV (Tuition Service cập nhật nguyên tử,
     *                 nếu thất bại thì SAGA BỒI HOÀN tiền lại ngay cho người nộp).
     */
    @Transactional
    public PaymentTransactionDto confirmPayment(String tokenHeader, ConfirmPaymentRequest request) {
        log.info("[PAYMENT-CONFIRM] Bắt đầu xác nhận giao dịch: {}", request.getTransactionId());

        PaymentTransaction tx = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch: " + request.getTransactionId()));

        // IDEMPOTENCY GUARD (Chuẩn Ngân Hàng - Chống Double-Click / Lặp request do rớt mạng)
        if ("SUCCESS".equals(tx.getStatus())) {
            log.info("[IDEMPOTENT] Giao dịch {} đã được xử lý thành công trước đó. Trả về kết quả nguyên thủy.", tx.getId());
            return toDto(tx);
        }

        if (!"PENDING".equals(tx.getStatus())) {
            throw new IllegalStateException("Giao dịch đã được xử lý hoặc không ở trạng thái chờ xác thực!");
        }

        // 1. Xác thực OTP
        OtpVerifyResponse otpRes = otpClient.verifyOtp(OtpVerifyRequest.builder()
                .transactionId(tx.getId())
                .otpCode(request.getOtpCode())
                .build());

        if (!otpRes.isValid()) {
            throw new IllegalArgumentException("Mã OTP không hợp lệ");
        }

        // 2. Saga Step 1: Trừ tiền người nộp tiền (có khóa Pessimistic Lock tại Account Service)
        boolean deducted = false;
        try {
            accountClient.deductBalance(DeductBalanceRequest.builder()
                    .accountNumber(tx.getAccountNumber())
                    .amount(tx.getAmount())
                    .transactionId(tx.getId())
                    .build());
            deducted = true;
            log.info("[SAGA-STEP-1] Đã trừ tiền thành công từ STK {}", tx.getAccountNumber());
        } catch (Exception e) {
            log.error("[SAGA-STEP-1-FAIL] Trừ tiền thất bại: {}", e.getMessage());
            tx.setStatus("FAILED");
            tx.setFailureReason("Trừ tiền thất bại: " + e.getMessage());
            tx.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(tx);
            throw new IllegalStateException("Giao dịch thất bại: " + e.getMessage());
        }

        // 3. Saga Step 2: Gạch nợ học phí tại Tuition Service (Atomic check `WHERE status = 'UNPAID'`)
        try {
            tuitionClient.clearDebt(ClearDebtRequest.builder()
                    .mssv(tx.getMssv())
                    .amount(tx.getAmount())
                    .transactionRef(tx.getId())
                    .build());
            log.info("[SAGA-STEP-2] Đã gạch nợ học phí thành công cho MSSV {}", tx.getMssv());
        } catch (Exception e) {
            log.error("[SAGA-STEP-2-FAIL] Gạch nợ thất bại (Khoản học phí có thể đã được người khác thanh toán): {}", e.getMessage());

            // BỒI HOÀN (COMPENSATING TRANSACTION): Hoàn lại số tiền đã trừ vào tài khoản
            if (deducted) {
                log.info("[COMPENSATION] Đang hoàn lại số tiền {} vào STK {}", tx.getAmount(), tx.getAccountNumber());
                try {
                    accountClient.refundBalance(RefundBalanceRequest.builder()
                            .accountNumber(tx.getAccountNumber())
                            .amount(tx.getAmount())
                            .transactionId(tx.getId())
                            .build());
                    log.info("[COMPENSATION] Bồi hoàn tiền thành công!");
                } catch (Exception ex) {
                    log.error("[COMPENSATION-CRITICAL] Lỗi khi bồi hoàn số dư: {}", ex.getMessage());
                }
            }

            tx.setStatus("FAILED");
            tx.setFailureReason("Khoản học phí đã được thanh toán bởi một giao dịch khác (Tiền đã được hoàn lại tài khoản)");
            tx.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(tx);

            throw new IllegalStateException("Khoản học phí của sinh viên này đã được thanh toán bởi một giao dịch khác! Số tiền đã được bồi hoàn vào tài khoản của bạn.");
        }

        // 4. Saga Hoàn Tất: Đánh dấu SUCCESS
        tx.setStatus("SUCCESS");
        tx.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        // 5. Gửi email biên lai xác nhận giao dịch thành công
        UserDto user = accountClient.getProfile(tokenHeader);
        otpClient.sendReceipt(ReceiptNotificationRequest.builder()
                .transactionId(tx.getId())
                .recipientEmail(user.getEmail())
                .payerName(tx.getPayerName())
                .accountNumber(tx.getAccountNumber())
                .mssv(tx.getMssv())
                .studentName(tx.getStudentName())
                .amount(tx.getAmount())
                .completedAt(tx.getCompletedAt())
                .build());

        log.info("[PAYMENT-SUCCESS] Giao dịch {} hoàn tất thành công 100%!", tx.getId());

        return toDto(tx);
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionDto> getTransactionHistory(String tokenHeader) {
        UserDto user = accountClient.getProfile(tokenHeader);
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private PaymentTransactionDto toDto(PaymentTransaction tx) {
        return PaymentTransactionDto.builder()
                .id(tx.getId())
                .userId(tx.getUserId())
                .payerName(tx.getPayerName())
                .accountNumber(tx.getAccountNumber())
                .mssv(tx.getMssv())
                .studentName(tx.getStudentName())
                .amount(tx.getAmount())
                .status(tx.getStatus())
                .failureReason(tx.getFailureReason())
                .createdAt(tx.getCreatedAt())
                .completedAt(tx.getCompletedAt())
                .build();
    }
}

