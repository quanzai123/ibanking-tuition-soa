package com.tdtu.soa.otp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(nullable = false, length = 10)
    private String otpCode;

    @Column(nullable = false, length = 100)
    private String recipientEmail;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean isUsed = false;

    @Column(nullable = false)
    @Builder.Default
    private int failedAttempts = 0;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

