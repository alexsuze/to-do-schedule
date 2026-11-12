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
    private final CompanyMigrator companies;

    public LegacyDataMigrator(MongoTemplate mongo, ProjectMigrator projects, CompanyMigrator companies) {
        this.mongo = mongo;
        this.projects = projects;
        this.companies = companies;
    }

    /** 每位 active 使用者登入時：確保至少有一家公司，沒有公司的專案 / 任務歸過去（需求書 12.8）。 */
    public void prepare(String userId) {
        companies.migrate(userId);
    }

    public void claim(String adminId) {
        for (String collection : List.of("tasks", "categories", "time_blocks")) {
            mongo.updateMulti(query(where("user_id").exists(false)), Update.update("user_id", adminId), collection);
        }
        projects.migrate(adminId); // 剛歸屬的分類也要掛上專案
        companies.migrate(adminId);
        Settings legacy = mongo.findById(LEGACY_SETTINGS_ID, Settings.class);
        if (legacy == null) return;
        if (mongo.findById(adminId, Settings.class) == null) {
            legacy.id = adminId;
            mongo.save(legacy);
        }
        mongo.remove(query(where("_id").is(LEGACY_SETTINGS_ID)), Settings.class);
    }
}
