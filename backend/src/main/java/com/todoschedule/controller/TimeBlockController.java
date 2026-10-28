package com.todoschedule.controller;

import com.todoschedule.config.AuthFilter;
import com.todoschedule.model.TimeBlock;
import com.todoschedule.model.User;
import com.todoschedule.service.TimeBlockService;
import com.todoschedule.service.TimeBlockService.BlockRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 時程表時段 API（只限目前登入者自己的時段）。
 */
@RestController
@RequestMapping("/api/blocks")
public class TimeBlockController {

    private final TimeBlockService service;

    public TimeBlockController(TimeBlockService service) {
        this.service = service;
    }

    @GetMapping
    public List<TimeBlock> list(@RequestAttribute(AuthFilter.USER) User user) {
        return service.list(user.id);
    }

    @PostMapping
    public TimeBlock create(@RequestAttribute(AuthFilter.USER) User user, @RequestBody BlockRequest req) {
        return service.create(user.id, req);
    }

    @PutMapping("/{id}")
    public TimeBlock update(@RequestAttribute(AuthFilter.USER) User user, @PathVariable String id, @RequestBody BlockRequest req) {
        return service.update(user.id, id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@RequestAttribute(AuthFilter.USER) User user, @PathVariable String id) {
        service.delete(user.id, id);
    }
}
