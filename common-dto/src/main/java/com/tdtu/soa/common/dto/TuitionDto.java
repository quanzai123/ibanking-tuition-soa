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
public class TuitionDto {
    private String mssv;
    private String studentName;
    private String faculty;
    private String major;
    private String semester;
    private BigDecimal amount;
    private String status; // "UNPAID", "PAID"
    private LocalDateTime paidAt;
    private String transactionRef;
}

