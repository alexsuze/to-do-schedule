package com.todoschedule.controller;

import com.todoschedule.model.Task;
import com.todoschedule.service.TaskService;
import com.todoschedule.service.TaskService.TaskRequest;
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
 * 任務與回收桶 API。
 */
@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/tasks")
    public List<Task> list() {
        return service.list();
    }

    @PostMapping("/tasks")
    public Task create(@RequestBody TaskRequest req) {
        return service.create(req);
    }

    @PutMapping("/tasks/{id}")
    public Task update(@PathVariable String id, @RequestBody TaskRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/tasks/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    @GetMapping("/trash")
    public List<Task> trash() {
        return service.trash();
    }

    @PostMapping("/trash/{id}/restore")
    public TaskService.RestoreResult restore(@PathVariable String id) {
        return service.restore(id);
    }

    @DeleteMapping("/trash/{id}")
    public void purge(@PathVariable String id) {
        service.purge(id);
    }

    @DeleteMapping("/trash")
    public void purgeAll() {
        service.purgeAll();
    }
}
