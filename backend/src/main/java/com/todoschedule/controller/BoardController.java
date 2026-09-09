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
    public record BoardRequest(String name, String color, String lastViewMode) {}

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
        return service.update(id, req.name(), req.color(), req.lastViewMode());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    /** body 為完整欄位陣列，整批覆寫。 */
    @PutMapping("/{id}/columns")
    public Board updateColumns(@PathVariable String id, @RequestBody List<Column> columns) {
        return service.updateColumns(id, columns);
    }
}
