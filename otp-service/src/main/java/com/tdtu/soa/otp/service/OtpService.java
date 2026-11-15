package com.tdtu.soa.otp.service;

import com.tdtu.soa.common.dto.*;
import com.tdtu.soa.otp.entity.OtpRecord;
import com.tdtu.soa.otp.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailLogService emailLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${notification.otp-expiration-seconds:300}")
    private int otpExpirationSeconds; // 300s = 5 phút

    /**
     * SINH MÃ OTP:
     * - Gắn với một giao dịch cụ thể (transactionId)
     * - Không trùng với OTP đang có hiệu lực của giao dịch khác
     * - Thời hạn 5 phút
     * - Gửi qua email người nộp tiền
     */
    @Transactional
    public OtpGenerateResponse generateOtp(OtpGenerateRequest request) {
        log.info("[OTP-GENERATE] Yêu cầu sinh OTP cho TxId: {} - Email: {}",
                request.getTransactionId(), request.getRecipientEmail());

        // Xóa hoặc vô hiệu hóa OTP cũ của chính transactionId này nếu người dùng yêu cầu gửi lại
        otpRepository.findByTransactionId(request.getTransactionId()).ifPresent(old -> {
            old.setUsed(true);
            otpRepository.save(old);
        });

        // Sinh mã 6 chữ số ngẫu nhiên không trùng với OTP còn hiệu lực khác
        String otpCode;
        LocalDateTime now = LocalDateTime.now();
        int attempts = 0;
        do {
            otpCode = String.format("%06d", secureRandom.nextInt(1000000));
            attempts++;
            if (attempts > 50) {
                break; // An toàn tránh vòng lặp vô hạn
            }
        } while (otpRepository.existsByOtpCodeAndExpiresAtAfterAndIsUsedFalse(otpCode, now));

        LocalDateTime expiresAt = now.plusSeconds(otpExpirationSeconds);

        OtpRecord record = OtpRecord.builder()
                .transactionId(request.getTransactionId())
                .otpCode(otpCode)
                .recipientEmail(request.getRecipientEmail())
                .expiresAt(expiresAt)
                .isUsed(false)
                .build();
        otpRepository.save(record);

        // Gửi email OTP
        String emailSubject = "[iBanking TDTU] Mã xác thực OTP cho giao dịch đóng học phí";
        String emailContent = String.format(
                "Kính chào Quý khách,\n\n" +
                "Quý khách đang thực hiện giao dịch thanh toán học phí trên ứng dụng iBanking TDTU.\n" +
                "Mã xác thực OTP của Quý khách là: %s\n\n" +
                "- Mã OTP có hiệu lực trong vòng 5 phút (đến %s).\n" +
                "- Mã này chỉ được sử dụng duy nhất một lần.\n" +
                "- Vui lòng tuyệt đối không chia sẻ mã này cho bất kỳ ai để bảo vệ tài khoản.\n\n" +
                "Trân trọng,\nNgân hàng số iBanking TDTU",
                otpCode, expiresAt.toString()
        );

        emailLogService.logEmail(request.getRecipientEmail(), emailSubject, emailContent, "OTP", otpCode);

        // Mask email: VD: an.nguyen@example.com -> a***n@example.com
        String maskedEmail = maskEmail(request.getRecipientEmail());

        return OtpGenerateResponse.builder()
                .success(true)
                .expiresInSeconds(otpExpirationSeconds)
                .maskedEmail(maskedEmail)
                .debugOtpCode(otpCode)
                .build();
    }

    /**
     * XÁC THỰC MÃ OTP:
     * - Chỉ được sử dụng thành công một lần
     * - Bị từ chối nếu sai hoặc quá 5 phút
     */
    @Transactional
    public OtpVerifyResponse verifyOtp(OtpVerifyRequest request) {
        log.info("[OTP-VERIFY] Xác thực OTP cho TxId: {} - Mã nhập: {}", request.getTransactionId(), request.getOtpCode());

        OtpRecord record = otpRepository.findByTransactionId(request.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin OTP cho giao dịch này"));

        if (record.isUsed()) {
            log.warn("[OTP-VERIFY] Thất bại: Mã OTP đã từng được sử dụng trước đó hoặc đã bị khóa!");
            throw new IllegalStateException("Mã OTP này đã được sử dụng hoặc đã bị vô hiệu hóa vì lý do bảo mật!");
        }

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("[OTP-VERIFY] Thất bại: Mã OTP đã hết hạn 5 phút!");
            throw new IllegalStateException("Mã OTP đã hết hiệu lực (quá thời hạn 5 phút). Vui lòng yêu cầu mã mới!");
        }

        // Kiểm tra số lần nhập sai (Tối đa 3 lần theo chuẩn Ngân hàng)
        if (record.getFailedAttempts() >= 3) {
            record.setUsed(true);
            otpRepository.save(record);
            throw new IllegalStateException("Giao dịch đã bị hủy do nhập sai OTP quá 3 lần liên tiếp!");
        }

        // CONSTANT-TIME COMPARISON (MessageDigest.isEqual)
        // Chuẩn bảo mật ngân hàng: Chống Timing Attack (tấn công đo đạc thời gian)
        byte[] expectedBytes = record.getOtpCode().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] actualBytes = request.getOtpCode().trim().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        if (!java.security.MessageDigest.isEqual(expectedBytes, actualBytes)) {
            record.setFailedAttempts(record.getFailedAttempts() + 1);
            int remaining = 3 - record.getFailedAttempts();
            if (remaining <= 0) {
                record.setUsed(true); // Khóa vĩnh viễn giao dịch này
            }
            otpRepository.save(record);

            log.warn("[OTP-VERIFY] Nhập sai OTP. Số lần còn lại: {}", remaining);
            if (remaining <= 0) {
                throw new IllegalStateException("Nhập sai OTP quá 3 lần! Giao dịch này đã bị khóa hoàn toàn.");
            }
            throw new IllegalArgumentException("Mã OTP không chính xác. Bạn còn " + remaining + " lần thử!");
        }

        // Đánh dấu đã sử dụng thành công 1 lần duy nhất
        record.setUsed(true);
        otpRepository.save(record);

        log.info("[OTP-VERIFY] Xác thực thành công cho TxId: {}", request.getTransactionId());
        return OtpVerifyResponse.builder()
                .valid(true)
                .message("Xác thực OTP thành công")
                .build();
    }

    /**
     * GỬI EMAIL BIÊN LAI XÁC NHẬN GIAO DỊCH THÀNH CÔNG
     */
    public void sendReceipt(ReceiptNotificationRequest req) {
        log.info("[NOTIFICATION] Gửi email biên lai giao dịch thành công TxId: {} tới {}",
                req.getTransactionId(), req.getRecipientEmail());

        String emailSubject = "[iBanking TDTU] Biên lai xác nhận thanh toán học phí thành công";
        String emailContent = String.format(
                "Kính chào Quý khách %s,\n\n" +
                "Giao dịch thanh toán học phí TDTU của Quý khách đã được xử lý THÀNH CÔNG!\n\n" +
                "--------------------------------------------------\n" +
                "THÔNG TIN BIÊN LAI GIAO DỊCH:\n" +
                "- Mã giao dịch:         %s\n" +
                "- Thời gian thanh toán: %s\n" +
                "- Tài khoản trích tiền: %s\n" +
                "- Mã sinh viên (MSSV):  %s\n" +
                "- Tên sinh viên:        %s\n" +
                "- Số tiền thanh toán:   %s VNĐ\n" +
                "- Trạng thái học phí:   ĐÃ THANH TOÁN (GẠCH NỢ THÀNH CÔNG)\n" +
                "--------------------------------------------------\n\n" +
                "Cảm ơn Quý khách đã sử dụng dịch vụ iBanking TDTU.\n\n" +
                "Trân trọng,\nNgân hàng số iBanking TDTU",
                req.getPayerName(),
                req.getTransactionId(),
                req.getCompletedAt() != null ? req.getCompletedAt().toString() : LocalDateTime.now().toString(),
                req.getAccountNumber(),
                req.getMssv(),
                req.getStudentName(),
                String.format("%,.0f", req.getAmount())
        );

        emailLogService.logEmail(req.getRecipientEmail(), emailSubject, emailContent, "RECEIPT", null);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 2) return name.charAt(0) + "***@" + parts[1];
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + parts[1];
    }
}

