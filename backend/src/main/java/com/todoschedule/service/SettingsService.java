package com.todoschedule.service;

import com.todoschedule.config.AppProperties;
import com.todoschedule.model.Settings;
import com.todoschedule.model.User;
import com.todoschedule.repository.SettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 個人設定（需求書第 5 節、9.6）：每日摘要開關與寄送時間、測試信。收件人為帳號的 Google Email。
 */
@Service
public class SettingsService {

    public static final Set<Integer> ARCHIVE_DAYS = Set.of(7, 14, 30, 60, 90);
    private static final Pattern TIME = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private final SettingsRepository repo;
    private final EmailService email;
    private final AppProperties props;

    public SettingsService(SettingsRepository repo, EmailService email, AppProperties props) {
        this.repo = repo;
        this.email = email;
        this.props = props;
    }

    public record SettingsRequest(Boolean digestEnabled, String digestTime) {}

    public record ArchiveDaysRequest(Integer days) {}

    /** 設定頁要顯示的內容。 */
    public record SettingsView(boolean digestEnabled, String digestTime, String lastDigestResult,
                               String email, boolean digestAvailable, int archiveDays) {
        static SettingsView of(Settings s, User u, boolean available) {
            return new SettingsView(s.digestEnabled, s.digestTime, s.lastDigestResult, u.email, available, s.archiveDays);
        }
    }

    /** 還沒存過就回傳預設值。 */
    public Settings get(String userId) {
        return repo.findById(userId).orElseGet(() -> {
            Settings s = new Settings();
            s.id = userId;
            return s;
        });
    }

    public Settings save(Settings s) {
        return repo.save(s);
    }

    /** Resend 未驗證自有網域前，只有管理員收得到信。 */
    public boolean digestAvailable(User u) {
        return u.admin || props.mailAllUsers();
    }

    public SettingsView view(User u) {
        return view(u, get(u.id));
    }

    private SettingsView view(User u, Settings s) {
        return SettingsView.of(s, u, digestAvailable(u));
    }

    public SettingsView update(User u, SettingsRequest req) {
        boolean enabled = Boolean.TRUE.equals(req.digestEnabled());
        if (enabled && !digestAvailable(u)) throw notAvailable();
        if (req.digestTime() == null || !TIME.matcher(req.digestTime()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "寄送時間格式不正確");
        }
        Settings s = get(u.id);
        s.digestEnabled = enabled;
        s.digestTime = req.digestTime();
        return SettingsView.of(repo.save(s), u, true);
    }

    public SettingsView updateArchiveDays(User u, ArchiveDaysRequest req) {
        if (req.days() == null || !ARCHIVE_DAYS.contains(req.days())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "封存天數只能選 7、14、30、60 或 90 天");
        }
        Settings s = get(u.id);
        s.archiveDays = req.days();
        return view(u, repo.save(s));
    }

    public void sendTest(User u) {
        if (!digestAvailable(u)) throw notAvailable();
        try {
            email.send(u.email, "[測試] 工作事項管理系統",
                    "<p>這是一封測試信，收到代表寄信設定正確。</p>", "這是一封測試信，收到代表寄信設定正確。");
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }

    private static ResponseStatusException notAvailable() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "每日摘要目前僅限管理員使用");
    }
}
