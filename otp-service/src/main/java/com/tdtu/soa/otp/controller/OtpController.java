package com.tdtu.soa.otp.controller;

import com.tdtu.soa.common.dto.*;
import com.tdtu.soa.otp.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/internal/generate")
    public ResponseEntity<ApiResponse<OtpGenerateResponse>> generateOtp(@RequestBody OtpGenerateRequest request) {
        OtpGenerateResponse response = otpService.generateOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("Mã OTP đã được gửi đến email", response));
    }

    @PostMapping("/internal/verify")
    public ResponseEntity<ApiResponse<OtpVerifyResponse>> verifyOtp(@RequestBody OtpVerifyRequest request) {
        OtpVerifyResponse response = otpService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

