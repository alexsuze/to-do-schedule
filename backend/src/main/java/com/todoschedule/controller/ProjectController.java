package com.todoschedule.controller;

import com.todoschedule.config.AuthFilter;
import com.todoschedule.model.Project;
import com.todoschedule.model.User;
import com.todoschedule.service.ProjectService;
import com.todoschedule.service.ProjectService.ProjectRequest;
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
import java.util.Map;

/**
 * 專案 API（只限目前登入者自己的專案）。
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @GetMapping
    public List<Project> list(@RequestAttribute(AuthFilter.USER) User user) {
        return service.list(user.id);
    }

    @PostMapping
    public Project create(@RequestAttribute(AuthFilter.USER) User user, @RequestBody ProjectRequest req) {
        return service.create(user.id, req);
    }

    @PutMapping("/reorder")
    public List<Project> reorder(@RequestAttribute(AuthFilter.USER) User user, @RequestBody Map<String, List<String>> body) {
        return service.reorder(user.id, body.get("ids"));
    }

    @PutMapping("/{id}")
    public Project update(@RequestAttribute(AuthFilter.USER) User user, @PathVariable String id, @RequestBody ProjectRequest req) {
        return service.update(user.id, id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@RequestAttribute(AuthFilter.USER) User user, @PathVariable String id) {
        service.delete(user.id, id);
    }
}
