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
public class ReceiptNotificationRequest {
    private String transactionId;
    private String recipientEmail;
    private String payerName;
    private String accountNumber;
    private String mssv;
    private String studentName;
    private BigDecimal amount;
    private LocalDateTime completedAt;
}

