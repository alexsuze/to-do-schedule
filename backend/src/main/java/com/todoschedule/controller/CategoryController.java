package com.todoschedule.controller;

import com.todoschedule.config.AuthFilter;
import com.todoschedule.model.Category;
import com.todoschedule.model.User;
import com.todoschedule.service.CategoryService;
import com.todoschedule.service.CategoryService.CategoryRequest;
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
 * 分類 API（只限目前登入者自己的分類）。
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<Category> list(@RequestAttribute(AuthFilter.USER) User user) {
        return service.list(user.id);
    }

    @PostMapping
    public Category create(@RequestAttribute(AuthFilter.USER) User user, @RequestBody CategoryRequest req) {
        return service.create(user.id, req);
    }

    @PutMapping("/reorder")
    public List<Category> reorder(@RequestAttribute(AuthFilter.USER) User user, @RequestBody Map<String, List<String>> body) {
        return service.reorder(user.id, body.get("ids"));
    }

    @PutMapping("/{id}")
    public Category update(@RequestAttribute(AuthFilter.USER) User user, @PathVariable String id, @RequestBody CategoryRequest req) {
        return service.update(user.id, id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@RequestAttribute(AuthFilter.USER) User user, @PathVariable String id) {
        service.delete(user.id, id);
    }
}
