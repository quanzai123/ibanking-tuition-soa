package com.tdtu.soa.otp.controller;

import com.tdtu.soa.common.dto.ApiResponse;
import com.tdtu.soa.common.dto.ReceiptNotificationRequest;
import com.tdtu.soa.otp.service.EmailLogService;
import com.tdtu.soa.otp.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final OtpService otpService;
    private final EmailLogService emailLogService;

    @PostMapping("/internal/send-receipt")
    public ResponseEntity<ApiResponse<String>> sendReceipt(@RequestBody ReceiptNotificationRequest request) {
        otpService.sendReceipt(request);
        return ResponseEntity.ok(ApiResponse.ok("Biên lai đã được gửi đến email", "SENT"));
    }

    @GetMapping("/emails")
    public ResponseEntity<ApiResponse<List<EmailLogService.EmailMessage>>> getRecentEmails() {
        return ResponseEntity.ok(ApiResponse.ok(emailLogService.getRecentEmails()));
    }
}
