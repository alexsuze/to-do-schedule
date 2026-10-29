package com.todoschedule.service;

import com.todoschedule.model.Category;
import com.todoschedule.model.Project;
import com.todoschedule.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * 加入「專案」之前的分類沒有 project_id：每位使用者建立預設專案「一般」，把這些分類與其任務掛上（需求書 10.6）。
 * 已有專案的分類不受影響，重跑無害。
 */
@Component
public class ProjectMigrator {

    static final String DEFAULT_NAME = "一般";
    private static final String DEFAULT_COLOR = "#E2DCEB";
    private static final Logger log = LoggerFactory.getLogger(ProjectMigrator.class);

    private final MongoTemplate mongo;

    public ProjectMigrator(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /** 啟動後在背景跑一次，不擋啟動（本機沒有 Mongo 也能開）。 */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        new Thread(() -> {
            try {
                migrateAll();
            } catch (RuntimeException e) {
                log.warn("專案搬移失敗，下次啟動或管理員登入時會再試", e);
            }
        }, "project-migrator").start();
    }

    void migrateAll() {
        List<String> users = mongo.findDistinct(query(where("project_id").exists(false).and("user_id").exists(true)),
                "user_id", Category.class, String.class);
        users.forEach(this::migrate);
    }

    public void migrate(String userId) {
        List<Category> orphans = mongo.find(query(where("user_id").is(userId).and("project_id").exists(false)), Category.class);
        if (orphans.isEmpty()) return;

        Project p = mongo.findOne(query(where("user_id").is(userId).and("name").is(DEFAULT_NAME)), Project.class);
        if (p == null) {
            p = new Project();
            p.userId = userId;
            p.name = DEFAULT_NAME;
            p.color = DEFAULT_COLOR;
            p.order = (int) mongo.count(query(where("user_id").is(userId)), Project.class);
            mongo.save(p);
        }
        List<String> ids = orphans.stream().map(c -> c.id).toList();
        mongo.updateMulti(query(where("_id").in(ids)), Update.update("project_id", p.id), Category.class);
        mongo.updateMulti(query(where("category_id").in(ids)), Update.update("project_id", p.id), Task.class);
        log.info("使用者 {} 的 {} 個分類已移入專案「{}」", userId, ids.size(), DEFAULT_NAME);
    }
}
