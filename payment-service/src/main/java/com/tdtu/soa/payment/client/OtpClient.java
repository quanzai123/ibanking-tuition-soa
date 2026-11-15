package com.tdtu.soa.payment.client;

import com.tdtu.soa.common.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class OtpClient {

    private final RestClient restClient;

    @Value("${services.otp.url:http://localhost:8084}")
    private String otpServiceUrl;

    public OtpGenerateResponse generateOtp(OtpGenerateRequest request) {
        try {
            ApiResponse<OtpGenerateResponse> response = restClient.post()
                    .uri(otpServiceUrl + "/api/otp/internal/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<OtpGenerateResponse>>() {});

            if (response != null && response.isSuccess()) {
                return response.getData();
            }
            throw new IllegalStateException("Không thể sinh OTP: " + (response != null ? response.getMessage() : "null"));
        } catch (Exception e) {
            log.error("Lỗi khi gọi Otp Service generateOtp: {}", e.getMessage());
            throw new IllegalStateException("Lỗi dịch vụ OTP: " + e.getMessage());
        }
    }

    public OtpVerifyResponse verifyOtp(OtpVerifyRequest request) {
        try {
            ApiResponse<OtpVerifyResponse> response = restClient.post()
                    .uri(otpServiceUrl + "/api/otp/internal/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        // Forward the error message from OTP service
                        throw new IllegalArgumentException("Xác thực OTP thất bại (Sai mã hoặc hết hạn 5 phút)");
                    })
                    .body(new ParameterizedTypeReference<ApiResponse<OtpVerifyResponse>>() {});

            if (response != null && response.isSuccess() && response.getData().isValid()) {
                return response.getData();
            }
            throw new IllegalArgumentException(response != null ? response.getMessage() : "Mã OTP không hợp lệ");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi gọi Otp Service verifyOtp: {}", e.getMessage());
            throw new IllegalArgumentException("Mã OTP không hợp lệ hoặc đã hết hạn 5 phút");
        }
    }

    public void sendReceipt(ReceiptNotificationRequest request) {
        try {
            restClient.post()
                    .uri(otpServiceUrl + "/api/notifications/internal/send-receipt")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Đã yêu cầu Otp Service gửi email biên lai cho Tx: {}", request.getTransactionId());
        } catch (Exception e) {
            log.warn("Không thể gửi email biên lai: {}", e.getMessage());
        }
    }
}

