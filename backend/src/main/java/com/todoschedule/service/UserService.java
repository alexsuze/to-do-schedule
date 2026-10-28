package com.todoschedule.service;

import com.todoschedule.config.AppProperties;
import com.todoschedule.model.User;
import com.todoschedule.repository.UserRepository;
import com.todoschedule.service.GoogleVerifier.GoogleUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * 登入、驗證通行證、管理員核准 / 停用（需求書 9.2、9.3）。
 */
@Service
public class UserService {

    private static final Set<String> STATUSES = Set.of(User.PENDING, User.ACTIVE, User.DISABLED);

    private final UserRepository repo;
    private final GoogleVerifier google;
    private final TokenService tokens;
    private final LegacyDataMigrator migrator;
    private final AppProperties props;

    public UserService(UserRepository repo, GoogleVerifier google, TokenService tokens,
                       LegacyDataMigrator migrator, AppProperties props) {
        this.repo = repo;
        this.google = google;
        this.tokens = tokens;
        this.migrator = migrator;
        this.props = props;
    }

    public record LoginResult(String token, User user) {}

    /** 用 Google 身分憑證登入；第一次登入建立待核准帳號。待核准 / 停用回 403。 */
    public LoginResult login(String credential) {
        GoogleUser g = google.verify(credential);
        User u = repo.findByGoogleSub(g.sub()).orElseGet(User::new);
        if (u.id == null) {
            u.googleSub = g.sub();
            u.createdAt = Instant.now();
        }
        u.email = g.email();
        u.name = g.name();
        u.picture = g.picture();
        u.lastLoginAt = Instant.now();
        if (isAdminEmail(g.email())) {
            u.admin = true;
            u.status = User.ACTIVE; // 管理員不會被鎖在外面
        }
        repo.save(u);
        if (u.admin) migrator.claim(u.id);

        if (User.PENDING.equals(u.status)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "已送出申請，請等待管理員核准後再登入");
        }
        if (User.DISABLED.equals(u.status)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "此帳號已停用，請聯絡管理員");
        }
        return new LoginResult(tokens.issue(u.id), u);
    }

    /** 通行證有效且帳號使用中才回傳使用者，否則 null。 */
    public User authenticate(String token) {
        String id = token == null ? null : tokens.verify(token);
        if (id == null) return null;
        return repo.findById(id).filter(u -> User.ACTIVE.equals(u.status)).orElse(null);
    }

    public List<User> list(User admin) {
        requireAdmin(admin);
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public User setStatus(User admin, String id, String status) {
        requireAdmin(admin);
        if (!STATUSES.contains(status)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "狀態不正確");
        if (admin.id.equals(id)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能變更自己的狀態");
        User u = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到此使用者"));
        u.status = status;
        return repo.save(u);
    }

    public List<User> active() {
        return repo.findByStatus(User.ACTIVE);
    }

    private boolean isAdminEmail(String email) {
        String admin = props.auth().adminEmail();
        return admin != null && !admin.isBlank() && admin.trim().equalsIgnoreCase(email);
    }

    private static void requireAdmin(User u) {
        if (!u.admin) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有管理員可以使用");
    }
}
