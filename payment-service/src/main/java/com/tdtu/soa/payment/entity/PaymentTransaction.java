package com.tdtu.soa.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {
    @Id
    @Column(length = 64, nullable = false)
    private String id; // UUID mã GD

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String payerName;

    @Column(nullable = false, length = 30)
    private String accountNumber;

    @Column(nullable = false, length = 20)
    private String mssv;

    @Column(nullable = false, length = 100)
    private String studentName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String status; // "PENDING", "SUCCESS", "FAILED"

    @Column(length = 255)
    private String failureReason;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;
}

