package com.todoschedule.service;

import com.todoschedule.model.Settings;
import com.todoschedule.repository.SettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 全域設定（需求書 3.5.1）。
 */
@Service
public class SettingsService {

    private static final Pattern HHMM = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /** 只更新非 null 欄位。 */
    public record SettingsRequest(String notifyEmail, Boolean notificationsEnabled, List<String> defaultLeadTimes,
                                  Boolean dailyDigestEnabled, String dailyDigestTime, String timezone,
                                  String lastBoardId) {}

    private final SettingsRepository repo;
    private final EmailService email;

    public SettingsService(SettingsRepository repo, EmailService email) {
        this.repo = repo;
        this.email = email;
    }

    public Settings get() {
        return repo.findById(Settings.ID).orElseGet(() -> repo.save(new Settings()));
    }

    public Settings update(SettingsRequest req) {
        Settings s = get();
        if (req.notifyEmail() != null) {
            String v = req.notifyEmail().strip();
            if (!v.isEmpty() && !EMAIL.matcher(v).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email 格式不正確");
            }
            s.notifyEmail = v.isEmpty() ? null : v;
        }
        if (req.notificationsEnabled() != null) s.notificationsEnabled = req.notificationsEnabled();
        if (req.defaultLeadTimes() != null) s.defaultLeadTimes = validLeadTimes(req.defaultLeadTimes());
        if (req.dailyDigestEnabled() != null) s.dailyDigestEnabled = req.dailyDigestEnabled();
        if (req.dailyDigestTime() != null) {
            if (!HHMM.matcher(req.dailyDigestTime()).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "每日摘要時間格式需為 HH:mm");
            }
            s.dailyDigestTime = req.dailyDigestTime();
        }
        if (req.timezone() != null) {
            try { ZoneId.of(req.timezone()); } catch (DateTimeException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "時區不正確");
            }
            s.timezone = req.timezone();
        }
        if (req.lastBoardId() != null) s.lastBoardId = req.lastBoardId().isBlank() ? null : req.lastBoardId();
        return repo.save(s);
    }

    /** 寄送測試信，回傳 Resend 訊息 id。 */
    public String sendTest() {
        Settings s = get();
        try {
            return email.send(s.notifyEmail, "[工作提醒] 測試信",
                    "<p>這是工作事項管理系統的測試信，收到代表 Email 通知設定正確。</p>",
                    "這是工作事項管理系統的測試信，收到代表 Email 通知設定正確。");
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }

    public static List<String> validLeadTimes(List<String> list) {
        List<String> clean = list.stream().distinct().toList();
        for (String l : clean) {
            if (!Settings.LEAD_TIMES.contains(l)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "提前時間不正確：" + l);
            }
        }
        return clean;
    }
}
