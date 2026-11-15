package com.tdtu.soa.payment.controller;

import com.tdtu.soa.common.dto.*;
import com.tdtu.soa.payment.service.PaymentSagaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentSagaService paymentSagaService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<InitiatePaymentResponse>> initiatePayment(
            @RequestHeader("Authorization") String tokenHeader,
            @RequestBody InitiatePaymentRequest request) {
        InitiatePaymentResponse response = paymentSagaService.initiatePayment(tokenHeader, request);
        return ResponseEntity.ok(ApiResponse.ok("Khởi tạo giao dịch thành công. Mã OTP đã được gửi.", response));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> confirmPayment(
            @RequestHeader("Authorization") String tokenHeader,
            @RequestBody ConfirmPaymentRequest request) {
        PaymentTransactionDto response = paymentSagaService.confirmPayment(tokenHeader, request);
        return ResponseEntity.ok(ApiResponse.ok("Thanh toán học phí thành công!", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PaymentTransactionDto>>> getHistory(
            @RequestHeader("Authorization") String tokenHeader) {
        List<PaymentTransactionDto> history = paymentSagaService.getTransactionHistory(tokenHeader);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}

