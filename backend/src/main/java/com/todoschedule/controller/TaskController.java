package com.todoschedule.controller;

import com.todoschedule.config.AuthFilter;
import com.todoschedule.model.Task;
import com.todoschedule.model.User;
import com.todoschedule.service.TaskService;
import com.todoschedule.service.TaskService.RestoreResult;
import com.todoschedule.service.TaskService.TaskRequest;
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
 * 任務與回收桶 API（只限目前登入者自己的資料）。
 */
@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/tasks")
    public List<Task> list(@RequestAttribute(AuthFilter.USER) User user) {
        return service.list(user.id);
    }

    @PostMapping("/tasks")
    public Task create(@RequestAttribute(AuthFilter.USER) User user, @RequestBody TaskRequest req) {
        return service.create(user.id, req);
    }

    @PutMapping("/tasks/{id}")
    public Task update(@RequestAttribute(AuthFilter.USER) User user, @PathVariable String id, @RequestBody TaskRequest req) {
        return service.update(user.id, id, req);
    }

    @DeleteMapping("/tasks/{id}")
    public void delete(@RequestAttribute(AuthFilter.USER) User user, @PathVariable String id) {
        service.delete(user.id, id);
    }

    @GetMapping("/trash")
    public List<Task> trash(@RequestAttribute(AuthFilter.USER) User user) {
        return service.trash(user.id);
    }

    @PostMapping("/trash/{id}/restore")
    public RestoreResult restore(@RequestAttribute(AuthFilter.USER) User user, @PathVariable String id) {
        return service.restore(user.id, id);
    }

    @DeleteMapping("/trash/{id}")
    public void purge(@RequestAttribute(AuthFilter.USER) User user, @PathVariable String id) {
        service.purge(user.id, id);
    }

    @DeleteMapping("/trash")
    public void purgeAll(@RequestAttribute(AuthFilter.USER) User user) {
        service.purgeAll(user.id);
    }
}
