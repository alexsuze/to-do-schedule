package com.todoschedule.controller;

import com.todoschedule.model.Settings;
import com.todoschedule.service.SettingsService;
import com.todoschedule.service.SettingsService.SettingsRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 設定 API。
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService service;

    public SettingsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping
    public Settings get() {
        return service.get();
    }

    @PutMapping
    public Settings update(@RequestBody SettingsRequest req) {
        return service.update(req);
    }

    @PostMapping("/test-email")
    public void testEmail() {
        service.sendTest();
    }
}
