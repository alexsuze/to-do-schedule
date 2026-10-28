package com.todoschedule.controller;

import com.todoschedule.model.TimeBlock;
import com.todoschedule.service.TimeBlockService;
import com.todoschedule.service.TimeBlockService.BlockRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 時程表時段 API。
 */
@RestController
@RequestMapping("/api/blocks")
public class TimeBlockController {

    private final TimeBlockService service;

    public TimeBlockController(TimeBlockService service) {
        this.service = service;
    }

    @GetMapping
    public List<TimeBlock> list() {
        return service.list();
    }

    @PostMapping
    public TimeBlock create(@RequestBody BlockRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public TimeBlock update(@PathVariable String id, @RequestBody BlockRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
