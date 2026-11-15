package com.tdtu.soa.account.controller;

import com.tdtu.soa.account.service.AccountService;
import com.tdtu.soa.common.dto.ApiResponse;
import com.tdtu.soa.common.dto.DeductBalanceRequest;
import com.tdtu.soa.common.dto.RefundBalanceRequest;
import com.tdtu.soa.common.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getProfile(
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Vui lòng đăng nhập"));
        }
        UserDto userDto = accountService.getProfileFromToken(token);
        return ResponseEntity.ok(ApiResponse.ok(userDto));
    }

    @GetMapping("/internal/{accountNumber}")
    public ResponseEntity<ApiResponse<UserDto>> getAccountByNumber(@PathVariable String accountNumber) {
        UserDto userDto = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(ApiResponse.ok(userDto));
    }

    @PostMapping("/internal/deduct")
    public ResponseEntity<ApiResponse<UserDto>> deductBalance(@RequestBody DeductBalanceRequest request) {
        UserDto updated = accountService.deductBalance(request);
        return ResponseEntity.ok(ApiResponse.ok("Trừ tiền thành công", updated));
    }

    @PostMapping("/internal/refund")
    public ResponseEntity<ApiResponse<UserDto>> refundBalance(@RequestBody RefundBalanceRequest request) {
        UserDto updated = accountService.refundBalance(request);
        return ResponseEntity.ok(ApiResponse.ok("Bồi hoàn tiền thành công", updated));
    }

    @PostMapping("/internal/reset")
    public ResponseEntity<ApiResponse<String>> resetData() {
        accountService.resetSeedData();
        return ResponseEntity.ok(ApiResponse.ok("Dữ liệu tài khoản đã được đặt lại thành công", "RESET_OK"));
    }
}

