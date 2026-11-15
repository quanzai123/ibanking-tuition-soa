package com.tdtu.soa.payment.client;

import com.tdtu.soa.common.dto.ApiResponse;
import com.tdtu.soa.common.dto.ClearDebtRequest;
import com.tdtu.soa.common.dto.TuitionDto;
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
public class TuitionClient {

    private final RestClient restClient;

    @Value("${services.tuition.url:http://localhost:8083}")
    private String tuitionServiceUrl;

    public TuitionDto getTuition(String mssv) {
        try {
            ApiResponse<TuitionDto> response = restClient.get()
                    .uri(tuitionServiceUrl + "/api/tuitions/" + mssv)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new IllegalArgumentException("Không tìm thấy thông tin học phí cho MSSV: " + mssv);
                    })
                    .body(new ParameterizedTypeReference<ApiResponse<TuitionDto>>() {});

            if (response != null && response.isSuccess()) {
                return response.getData();
            }
            throw new IllegalArgumentException(response != null ? response.getMessage() : "Không tìm thấy học phí");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi kết nối Tuition Service: {}", e.getMessage());
            throw new IllegalStateException("Lỗi kết nối Tuition Service: " + e.getMessage());
        }
    }

    public TuitionDto clearDebt(ClearDebtRequest request) {
        try {
            ApiResponse<TuitionDto> response = restClient.post()
                    .uri(tuitionServiceUrl + "/api/tuitions/internal/clear-debt")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.value() == 409, (req, res) -> {
                        log.warn("[SAGA] Tuition Service trả về 409 Conflict cho MSSV: {}", request.getMssv());
                        throw new IllegalStateException("CONFLICT_TUITION_ALREADY_PAID");
                    })
                    .body(new ParameterizedTypeReference<ApiResponse<TuitionDto>>() {});

            if (response != null && response.isSuccess()) {
                return response.getData();
            }
            throw new IllegalStateException(response != null ? response.getMessage() : "Gạch nợ thất bại");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi gọi Tuition clearDebt: {}", e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }
    }
}

