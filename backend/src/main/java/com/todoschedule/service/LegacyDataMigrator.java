package com.todoschedule.service;

import com.todoschedule.model.Settings;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * 多人化之前的資料沒有 user_id，管理員登入時全部歸給管理員（需求書 9.5）。
 * 每次管理員登入都會執行；已歸屬的資料不受影響，所以重跑無害。
 */
@Component
public class LegacyDataMigrator {

    private static final String LEGACY_SETTINGS_ID = "app";

    private final MongoTemplate mongo;
    private final ProjectMigrator projects;

    public LegacyDataMigrator(MongoTemplate mongo, ProjectMigrator projects) {
        this.mongo = mongo;
        this.projects = projects;
    }

    public void claim(String adminId) {
        for (String collection : List.of("tasks", "categories", "time_blocks")) {
            mongo.updateMulti(query(where("user_id").exists(false)), Update.update("user_id", adminId), collection);
        }
        projects.migrate(adminId); // 剛歸屬的分類也要掛上專案
        Settings legacy = mongo.findById(LEGACY_SETTINGS_ID, Settings.class);
        if (legacy == null) return;
        if (mongo.findById(adminId, Settings.class) == null) {
            legacy.id = adminId;
            mongo.save(legacy);
        }
        mongo.remove(query(where("_id").is(LEGACY_SETTINGS_ID)), Settings.class);
    }
}
