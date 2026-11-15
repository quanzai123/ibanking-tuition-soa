package com.tdtu.soa.otp.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class EmailLogService {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailMessage {
        private String id;
        private String to;
        private String subject;
        private String content;
        private String type; // "OTP" or "RECEIPT"
        private String otpCode;
        @Builder.Default
        private LocalDateTime sentAt = LocalDateTime.now();
    }

    private final List<EmailMessage> sentEmails = Collections.synchronizedList(new ArrayList<>());

    public void logEmail(String to, String subject, String content, String type, String otpCode) {
        EmailMessage msg = EmailMessage.builder()
                .id(java.util.UUID.randomUUID().toString())
                .to(to)
                .subject(subject)
                .content(content)
                .type(type)
                .otpCode(otpCode)
                .sentAt(LocalDateTime.now())
                .build();
        sentEmails.add(0, msg); // newest first

        // Keep last 50 emails
        if (sentEmails.size() > 50) {
            sentEmails.remove(sentEmails.size() - 1);
        }

        log.info("==================== [EMAIL DISPATCHED] ====================");
        log.info("TO:      {}", to);
        log.info("SUBJECT: {}", subject);
        log.info("TYPE:    {}", type);
        if (otpCode != null) {
            log.info("OTP CODE: >>> {} <<< (Hiệu lực 5 phút)", otpCode);
        }
        log.info("CONTENT:\n{}", content);
        log.info("============================================================");
    }

    public List<EmailMessage> getRecentEmails() {
        return new ArrayList<>(sentEmails);
    }

    public void clear() {
        sentEmails.clear();
    }
}

