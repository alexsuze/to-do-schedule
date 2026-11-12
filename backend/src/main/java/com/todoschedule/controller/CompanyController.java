package com.todoschedule.controller;

import com.todoschedule.config.AuthFilter;
import com.todoschedule.model.Company;
import com.todoschedule.model.User;
import com.todoschedule.service.CompanyService;
import com.todoschedule.service.CompanyService.CompanyRequest;
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
 * 公司 API（只限目前登入者自己的公司）。不提供刪除，改用封存（需求書 12.3）。
 */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @GetMapping
    public List<Company> list(@RequestAttribute(AuthFilter.USER) User user) {
        return service.list(user.id);
    }

    @PostMapping
    public Company create(@RequestAttribute(AuthFilter.USER) User user, @RequestBody CompanyRequest req) {
        return service.create(user.id, req);
    }

    @PutMapping("/reorder")
    public List<Company> reorder(@RequestAttribute(AuthFilter.USER) User user, @RequestBody Map<String, List<String>> body) {
        return service.reorder(user.id, body.get("ids"));
    }

    @PutMapping("/{id}")
    public Company update(@RequestAttribute(AuthFilter.USER) User user, @PathVariable String id, @RequestBody CompanyRequest req) {
        return service.update(user.id, id, req);
    }
}
