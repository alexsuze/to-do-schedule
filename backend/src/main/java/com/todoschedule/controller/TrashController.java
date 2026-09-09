package com.todoschedule.controller;

import com.todoschedule.model.Board;
import com.todoschedule.model.Task;
import com.todoschedule.service.TrashService;
import com.todoschedule.service.TrashService.TrashedTask;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 回收桶 API。board_id 省略代表全部看板。
 */
@RestController
@RequestMapping("/api/trash")
public class TrashController {

    private final TrashService service;

    public TrashController(TrashService service) {
        this.service = service;
    }

    @GetMapping("/tasks")
    public List<TrashedTask> tasks(@RequestParam(name = "board_id", required = false) String boardId) {
        return service.listTasks(boardId);
    }

    @GetMapping("/boards")
    public List<Board> boards() {
        return service.listBoards();
    }

    @PostMapping("/tasks/{id}/restore")
    public Task restoreTask(@PathVariable String id) {
        return service.restoreTask(id);
    }

    @PostMapping("/boards/{id}/restore")
    public Board restoreBoard(@PathVariable String id) {
        return service.restoreBoard(id);
    }

    @DeleteMapping("/tasks/{id}")
    public void purgeTask(@PathVariable String id) {
        service.purgeTask(id);
    }

    @DeleteMapping("/boards/{id}")
    public void purgeBoard(@PathVariable String id) {
        service.purgeBoard(id);
    }

    @DeleteMapping
    public void empty(@RequestParam(name = "board_id", required = false) String boardId) {
        service.empty(boardId);
    }
}
