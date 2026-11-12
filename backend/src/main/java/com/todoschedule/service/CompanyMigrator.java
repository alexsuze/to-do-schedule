package com.todoschedule.service;

import com.todoschedule.model.Project;
import com.todoschedule.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * 加入「公司」之前的專案與任務沒有 company_id：歸到使用者排序第一家公司（沒有就建立「我的公司」），需求書 12.8。
 * 已有公司的資料不受影響，重跑無害。
 */
@Component
public class CompanyMigrator {

    private static final Logger log = LoggerFactory.getLogger(CompanyMigrator.class);

    private final MongoTemplate mongo;
    private final CompanyService companies;

    public CompanyMigrator(MongoTemplate mongo, CompanyService companies) {
        this.mongo = mongo;
        this.companies = companies;
    }

    void migrateAll() {
        Set<String> users = new LinkedHashSet<>();
        users.addAll(mongo.findDistinct(query(where("company_id").exists(false).and("user_id").exists(true)), "user_id", Project.class, String.class));
        users.addAll(mongo.findDistinct(query(where("company_id").exists(false).and("user_id").exists(true)), "user_id", Task.class, String.class));
        users.forEach(this::migrate);
    }

    /** 也確保使用者至少有一家公司。 */
    public void migrate(String userId) {
        String primary = companies.primary(userId).id;
        long projects = mongo.updateMulti(query(where("user_id").is(userId).and("company_id").exists(false)),
                Update.update("company_id", primary), Project.class).getModifiedCount();

        List<Task> orphans = mongo.find(query(where("user_id").is(userId).and("company_id").exists(false)), Task.class);
        if (orphans.isEmpty() && projects == 0) return;
        Map<String, String> companyOfProject = new HashMap<>();
        mongo.find(query(where("user_id").is(userId)), Project.class).forEach(p -> companyOfProject.put(p.id, p.companyId));
        for (Task t : orphans) {
            String c = t.projectId == null ? null : companyOfProject.get(t.projectId);
            mongo.updateFirst(query(where("_id").is(t.id)), Update.update("company_id", c == null ? primary : c), Task.class);
        }
        log.info("使用者 {} 的 {} 個專案、{} 個任務已歸到公司", userId, projects, orphans.size());
    }
}
