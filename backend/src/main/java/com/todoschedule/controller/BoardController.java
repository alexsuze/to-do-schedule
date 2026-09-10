package com.todoschedule.controller;

import com.todoschedule.model.Board;
import com.todoschedule.model.Board.Column;
import com.todoschedule.service.BoardService;
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
 * 看板 API。JSON 欄位皆為 snake_case（見 application.properties）。
 */
@RestController
@RequestMapping("/api/boards")
public class BoardController {

    /** name / color / last_view_mode 皆可省略，省略者不變更。 */
    public record BoardRequest(String name, String subtitle, String color, String lastViewMode) {}

    /** removed_tasks：被移除欄位內事項的處理，"delete" 或目標欄位 id，省略時若有事項會回 409。 */
    public record ColumnsRequest(List<Column> columns, String removedTasks) {}

    private final BoardService service;

    public BoardController(BoardService service) {
        this.service = service;
    }

    @GetMapping
    public List<Board> list() {
        return service.list();
    }

    @PostMapping
    public Board create(@RequestBody BoardRequest req) {
        return service.create(req.name(), req.color());
    }

    /** body 為看板 id 陣列，依陣列順序重排。 */
    @PutMapping("/reorder")
    public List<Board> reorder(@RequestBody List<String> ids) {
        return service.reorder(ids);
    }

    @PutMapping("/{id}")
    public Board update(@PathVariable String id, @RequestBody BoardRequest req) {
        return service.update(id, req.name(), req.subtitle(), req.color(), req.lastViewMode());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    /** body: { columns: [...], removed_tasks: ... }，整批覆寫欄位。 */
    @PutMapping("/{id}/columns")
    public Board updateColumns(@PathVariable String id, @RequestBody ColumnsRequest req) {
        return service.updateColumns(id, req.columns(), req.removedTasks());
    }
}
