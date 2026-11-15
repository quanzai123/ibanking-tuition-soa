package com.tdtu.soa.payment.client;

import com.tdtu.soa.common.dto.ApiResponse;
import com.tdtu.soa.common.dto.DeductBalanceRequest;
import com.tdtu.soa.common.dto.RefundBalanceRequest;
import com.tdtu.soa.common.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountClient {

    private final RestClient restClient;

    @Value("${services.account.url:http://localhost:8081}")
    private String accountServiceUrl;

    public UserDto getProfile(String token) {
        try {
            ApiResponse<UserDto> response = restClient.get()
                    .uri(accountServiceUrl + "/api/accounts/me")
                    .header("Authorization", token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<ApiResponse<UserDto>>() {
                    });

            if (response != null && response.isSuccess()) {
                return response.getData();
            }
            throw new IllegalStateException(
                    "Không thể lấy thông tin tài khoản: " + (response != null ? response.getMessage() : "null"));
        } catch (Exception e) {
            log.error("Lỗi khi gọi Account Service getProfile: {}", e.getMessage());
            throw new IllegalStateException("Lỗi khi kết nối đến Account Service: " + e.getMessage());
        }
    }

    public UserDto deductBalance(DeductBalanceRequest request) {
        try {
            ApiResponse<UserDto> response = restClient.post()
                    .uri(accountServiceUrl + "/api/accounts/internal/deduct")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<ApiResponse<UserDto>>() {
                    });

            if (response != null && response.isSuccess()) {
                return response.getData();
            }
            throw new IllegalStateException(response != null ? response.getMessage() : "Trừ tiền thất bại");
        } catch (Exception e) {
            log.error("Lỗi khi gọi Account Service deductBalance: {}", e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }
    }

    public UserDto refundBalance(RefundBalanceRequest request) {
        try {
            ApiResponse<UserDto> response = restClient.post()
                    .uri(accountServiceUrl + "/api/accounts/internal/refund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<ApiResponse<UserDto>>() {
                    });

            if (response != null && response.isSuccess()) {
                return response.getData();
            }
            throw new IllegalStateException(
                    "Bồi hoàn số dư thất bại: " + (response != null ? response.getMessage() : "null"));
        } catch (Exception e) {
            log.error("Lỗi khi gọi Account Service refundBalance: {}", e.getMessage());
            throw new IllegalStateException("Lỗi hoàn tiền: " + e.getMessage());
        }
    }
}
