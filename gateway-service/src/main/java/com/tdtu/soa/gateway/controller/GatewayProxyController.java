package com.tdtu.soa.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;

@RestController
@RequiredArgsConstructor
@Slf4j
public class GatewayProxyController {

    private final RestClient restClient;

    @Value("${services.account.url:http://localhost:8081}")
    private String accountServiceUrl;

    @Value("${services.payment.url:http://localhost:8082}")
    private String paymentServiceUrl;

    @Value("${services.tuition.url:http://localhost:8083}")
    private String tuitionServiceUrl;

    @Value("${services.otp.url:http://localhost:8084}")
    private String otpServiceUrl;

    @RequestMapping(value = "/api/**")
    public ResponseEntity<byte[]> proxy(
            @RequestBody(required = false) byte[] body,
            HttpMethod method,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        String targetBaseUrl = determineTargetService(path);

        String targetUrl = targetBaseUrl + path;
        if (queryString != null && !queryString.isBlank()) {
            targetUrl += "?" + queryString;
        }

        log.debug("[GATEWAY-ROUTING] {} {} -> {}", method, path, targetUrl);

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            for (String headerName : Collections.list(headerNames)) {
                if (!"host".equalsIgnoreCase(headerName) && !"content-length".equalsIgnoreCase(headerName)) {
                    headers.put(headerName, Collections.list(request.getHeaders(headerName)));
                }
            }
        }

        try {
            RestClient.RequestBodySpec requestSpec = restClient
                    .method(method)
                    .uri(URI.create(targetUrl))
                    .headers(h -> h.addAll(headers));

            if (body != null && body.length > 0) {
                requestSpec.body(body);
            }

            ResponseEntity<byte[]> response = requestSpec.retrieve().toEntity(byte[].class);

            HttpHeaders cleanHeaders = new HttpHeaders();
            if (response.getHeaders().getContentType() != null) {
                cleanHeaders.setContentType(response.getHeaders().getContentType());
            }

            return ResponseEntity.status(response.getStatusCode())
                    .headers(cleanHeaders)
                    .body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.warn("[GATEWAY-FORWARD-ERROR] Target {} returned status {}: {}", targetUrl, e.getStatusCode(),
                    e.getResponseBodyAsString());
            HttpHeaders cleanHeaders = new HttpHeaders();
            if (e.getResponseHeaders() != null && e.getResponseHeaders().getContentType() != null) {
                cleanHeaders.setContentType(e.getResponseHeaders().getContentType());
            }
            return ResponseEntity.status(e.getStatusCode())
                    .headers(cleanHeaders)
                    .body(e.getResponseBodyAsByteArray());
        } catch (Exception e) {
            log.error("[GATEWAY-SYSTEM-ERROR] Failed to forward to {}: {}", targetUrl, e.getMessage());
            String errorJson = String.format("{\"success\":false,\"message\":\"API Gateway Error: %s\"}",
                    e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorJson.getBytes());
        }
    }

    private String determineTargetService(String path) {
        if (path.startsWith("/api/auth") || path.startsWith("/api/accounts")) {
            return accountServiceUrl;
        } else if (path.startsWith("/api/payments")) {
            return paymentServiceUrl;
        } else if (path.startsWith("/api/tuitions")) {
            return tuitionServiceUrl;
        } else if (path.startsWith("/api/otp") || path.startsWith("/api/notifications")) {
            return otpServiceUrl;
        }
        return accountServiceUrl;
    }
}
