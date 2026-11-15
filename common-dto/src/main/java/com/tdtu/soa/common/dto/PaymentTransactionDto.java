package com.tdtu.soa.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionDto {
    private String id;
    private Long userId;
    private String payerName;
    private String accountNumber;
    private String mssv;
    private String studentName;
    private BigDecimal amount;
    private String status; // PENDING, SUCCESS, FAILED
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}

