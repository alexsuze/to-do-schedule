package com.todoschedule.controller;

import com.todoschedule.model.Task;
import com.todoschedule.service.TaskService;
import com.todoschedule.service.TaskService.MoveRequest;
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
import java.util.Map;

/**
 * 工作事項 API。列表與新增掛在看板底下，其餘以事項 id 操作。
 */
@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/boards/{boardId}/tasks")
    public List<Task> list(@PathVariable String boardId) {
        return service.list(boardId);
    }

    /** body: { "title": "..." } */
    @PostMapping("/boards/{boardId}/tasks")
    public Task create(@PathVariable String boardId, @RequestBody Map<String, String> body) {
        return service.create(boardId, body.get("title"));
    }

    @PutMapping("/tasks/{id}")
    public Task update(@PathVariable String id, @RequestBody TaskRequest req) {
        return service.update(id, req);
    }

    @PutMapping("/tasks/{id}/move")
    public Task move(@PathVariable String id, @RequestBody MoveRequest req) {
        return service.move(id, req);
    }

    @DeleteMapping("/tasks/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
