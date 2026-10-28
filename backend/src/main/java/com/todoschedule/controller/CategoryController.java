package com.todoschedule.controller;

import com.todoschedule.model.Category;
import com.todoschedule.service.CategoryService;
import com.todoschedule.service.CategoryService.CategoryRequest;
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
 * 分類 API。
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<Category> list() {
        return service.list();
    }

    @PostMapping
    public Category create(@RequestBody CategoryRequest req) {
        return service.create(req);
    }

    @PutMapping("/reorder")
    public List<Category> reorder(@RequestBody Map<String, List<String>> body) {
        return service.reorder(body.get("ids"));
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable String id, @RequestBody CategoryRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
