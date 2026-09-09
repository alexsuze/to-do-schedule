package com.todoschedule.controller;

import com.todoschedule.model.Notification;
import com.todoschedule.model.Settings;
import com.todoschedule.service.NotificationService;
import com.todoschedule.service.SettingsService;
import com.todoschedule.service.SettingsService.SettingsRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 全域設定與通知紀錄 API。
 */
@RestController
@RequestMapping("/api")
public class SettingsController {

    private final SettingsService settings;
    private final NotificationService notifications;

    public SettingsController(SettingsService settings, NotificationService notifications) {
        this.settings = settings;
        this.notifications = notifications;
    }

    @GetMapping("/settings")
    public Settings get() {
        return settings.get();
    }

    /** 只更新有傳入的欄位。 */
    @PutMapping("/settings")
    public Settings update(@RequestBody SettingsRequest req) {
        return settings.update(req);
    }

    @PostMapping("/settings/test-email")
    public Map<String, String> testEmail() {
        return Map.of("message_id", String.valueOf(settings.sendTest()));
    }

    /** 最近 50 筆通知紀錄。 */
    @GetMapping("/notifications")
    public List<Notification> notifications() {
        return notifications.recent();
    }
}
