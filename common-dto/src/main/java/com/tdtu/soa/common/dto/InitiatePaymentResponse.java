package com.tdtu.soa.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentResponse {
    private String transactionId;
    private String mssv;
    private String studentName;
    private BigDecimal amount;
    private String maskedEmail;
    private int expiresInSeconds;
    private String debugOtpCode;
}

