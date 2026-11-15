package com.tdtu.soa.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpGenerateResponse {
    private boolean success;
    private int expiresInSeconds;
    private String maskedEmail;
    // Helper field for local testing/demo only (can be null in strict prod)
    private String debugOtpCode;
}

