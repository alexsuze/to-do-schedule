package com.todoschedule.service;

import com.todoschedule.model.Settings;
import com.todoschedule.repository.SettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

/**
 * 設定（需求書第 5 節）：收件 Email、寄送時間、開關、測試信。
 */
@Service
public class SettingsService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern TIME = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private final SettingsRepository repo;
    private final EmailService email;

    public SettingsService(SettingsRepository repo, EmailService email) {
        this.repo = repo;
        this.email = email;
    }

    public record SettingsRequest(String email, Boolean digestEnabled, String digestTime) {}

    /** 還沒存過就回傳預設值。 */
    public Settings get() {
        return repo.findById(Settings.SINGLETON_ID).orElseGet(Settings::new);
    }

    public Settings save(Settings s) {
        return repo.save(s);
    }

    public Settings update(SettingsRequest req) {
        String mail = req.email() == null ? "" : req.email().trim();
        boolean enabled = Boolean.TRUE.equals(req.digestEnabled());
        if (!mail.isEmpty() && !EMAIL.matcher(mail).matches()) throw bad("Email 格式不正確");
        if (enabled && mail.isEmpty()) throw bad("開啟每日摘要需要填寫收件 Email");
        if (req.digestTime() == null || !TIME.matcher(req.digestTime()).matches()) throw bad("寄送時間格式不正確");

        Settings s = get();
        s.email = mail;
        s.digestEnabled = enabled;
        s.digestTime = req.digestTime();
        return repo.save(s);
    }

    /** 寄到已儲存的收件 Email。 */
    public void sendTest() {
        Settings s = get();
        if (s.email.isBlank()) throw bad("請先填寫並儲存收件 Email");
        try {
            email.send(s.email, "[測試] 工作事項管理系統",
                    "<p>這是一封測試信，收到代表寄信設定正確。</p>", "這是一封測試信，收到代表寄信設定正確。");
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
