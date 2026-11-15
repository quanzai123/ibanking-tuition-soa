package com.tdtu.soa.account.controller;

import com.tdtu.soa.account.service.AccountService;
import com.tdtu.soa.common.dto.ApiResponse;
import com.tdtu.soa.common.dto.LoginRequest;
import com.tdtu.soa.common.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AccountService accountService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse response = accountService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", response));
    }
}

