package com.todoschedule.service;

import com.todoschedule.config.AppProperties;
import com.todoschedule.config.AppProperties.Auth;
import com.todoschedule.model.User;
import com.todoschedule.repository.UserRepository;
import com.todoschedule.service.GoogleVerifier.GoogleUser;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 登入流程：新帳號待核准、管理員自動啟用並接收舊資料、停用擋下、核准後可登入、只有管理員能管理。 */
class UserServiceTest {

    private final UserRepository repo = mock(UserRepository.class);
    private final GoogleVerifier google = mock(GoogleVerifier.class);
    private final LegacyDataMigrator migrator = mock(LegacyDataMigrator.class);
    private final EmailService email = mock(EmailService.class);
    private final AppProperties props = new AppProperties(null, null, "https://app.example.test/",
            new Auth("cid", "Boss@Example.test", "0123456789abcdef0123456789abcdef"), false);
    private final TokenService tokens = new TokenService(props);
    private final UserService service = new UserService(repo, google, tokens, migrator, email, props);
    private final List<User> users = new ArrayList<>();

    UserServiceTest() {
        when(repo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.id == null) { u.id = "u" + (users.size() + 1); users.add(u); }
            return u;
        });
        when(repo.findById(anyString())).thenAnswer(inv -> users.stream().filter(u -> u.id.equals(inv.getArgument(0))).findFirst());
        when(repo.findByGoogleSub(anyString())).thenAnswer(inv -> users.stream().filter(u -> u.googleSub.equals(inv.getArgument(0))).findFirst());
        when(google.verify("boss")).thenReturn(new GoogleUser("g-boss", "boss@example.test", "Boss", null));
        when(google.verify("amy")).thenReturn(new GoogleUser("g-amy", "amy@example.test", "Amy", null));
    }

    @Test
    void adminIsActiveAndClaimsLegacyData() {
        UserService.LoginResult r = service.login("boss");
        assertTrue(r.user().admin);
        assertEquals(User.ACTIVE, r.user().status);
        assertEquals(r.user().id, service.authenticate(r.token()).id);
        verify(migrator).claim(r.user().id);
    }

    @Test
    void newUserWaitsForApprovalThenCanLogIn() {
        assertThrows(ResponseStatusException.class, () -> service.login("amy"), "待核准");
        User amy = users.get(0);
        assertEquals(User.PENDING, amy.status);
        verify(migrator, never()).claim(anyString());

        User boss = service.login("boss").user();
        assertThrows(ResponseStatusException.class, () -> service.setStatus(amy, boss.id, User.DISABLED), "非管理員不能管理");
        service.setStatus(boss, amy.id, User.ACTIVE);
        String token = service.login("amy").token();
        assertNotNull(service.authenticate(token));

        service.setStatus(boss, amy.id, User.DISABLED);
        assertNull(service.authenticate(token), "停用後舊通行證也失效");
        assertThrows(ResponseStatusException.class, () -> service.login("amy"));
    }

    @Test
    void newApplicationEmailsAdminOnce() {
        service.login("boss");
        verify(email, never()).send(anyString(), anyString(), anyString(), anyString());

        assertThrows(ResponseStatusException.class, () -> service.login("amy"));
        assertThrows(ResponseStatusException.class, () -> service.login("amy"), "還在等待，重複登入不再寄");
        verify(email, times(1)).send(eq("Boss@Example.test"), contains("amy@example.test"),
                contains("https://app.example.test/#/admin"), contains("Amy"));
    }

    @Test
    void emailFailureDoesNotBreakApplication() {
        doThrow(new IllegalStateException("Resend 回應 403")).when(email).send(anyString(), anyString(), anyString(), anyString());
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.login("amy"));
        assertTrue(e.getReason().contains("等待管理員核准"), "照常回應待核准，不是寄信錯誤");
        assertEquals(User.PENDING, users.get(0).status);
    }

    @Test
    void adminCannotChangeOwnStatus() {
        User boss = service.login("boss").user();
        assertThrows(ResponseStatusException.class, () -> service.setStatus(boss, boss.id, User.DISABLED));
        assertThrows(ResponseStatusException.class, () -> service.setStatus(boss, "nope", "weird"));
    }
}
