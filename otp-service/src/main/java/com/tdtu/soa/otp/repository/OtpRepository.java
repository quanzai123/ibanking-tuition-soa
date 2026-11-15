package com.tdtu.soa.otp.repository;

import com.tdtu.soa.otp.entity.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpRecord, Long> {
    Optional<OtpRecord> findByTransactionId(String transactionId);

    /**
     * Kiểm tra xem mã OTP này có đang còn hiệu lực trên bất kỳ giao dịch nào khác không
     */
    boolean existsByOtpCodeAndExpiresAtAfterAndIsUsedFalse(String otpCode, LocalDateTime now);
}

