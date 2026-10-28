package com.todoschedule.controller;

import com.todoschedule.config.AuthFilter;
import com.todoschedule.model.User;
import com.todoschedule.service.SettingsService;
import com.todoschedule.service.SettingsService.SettingsRequest;
import com.todoschedule.service.SettingsService.SettingsView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 個人設定 API。
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService service;

    public SettingsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping
    public SettingsView get(@RequestAttribute(AuthFilter.USER) User user) {
        return service.view(user);
    }

    @PutMapping
    public SettingsView update(@RequestAttribute(AuthFilter.USER) User user, @RequestBody SettingsRequest req) {
        return service.update(user, req);
    }

    @PostMapping("/test-email")
    public void testEmail(@RequestAttribute(AuthFilter.USER) User user) {
        service.sendTest(user);
    }
}
