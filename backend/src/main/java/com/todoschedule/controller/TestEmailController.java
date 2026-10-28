package com.todoschedule.controller;

import com.todoschedule.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 寄測試信，驗證 Resend 設定。body：{ "to": "收件 Email" }
 */
@RestController
@RequestMapping("/api/test-email")
public class TestEmailController {

    private final EmailService emailService;

    public TestEmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping
    public Map<String, Object> send(@RequestBody Map<String, String> body) {
        try {
            String id = emailService.send(body.get("to"), "[測試] 工作事項管理系統",
                    "<p>這是一封測試信，收到代表寄信設定正確。</p>", "這是一封測試信，收到代表寄信設定正確。");
            return Map.of("ok", true, "id", String.valueOf(id));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }
}
