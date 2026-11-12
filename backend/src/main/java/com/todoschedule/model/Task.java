package com.todoschedule.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 任務卡。欄位定義見 Doc/MongoDB資料結構.md。
 * ponytail: user_id / category_id 以 ObjectId 的 hex 字串儲存，不用 DBRef。
 */
@Document("tasks")
public class Task {

    @Id
    public String id;
    /** 擁有者（users 的 _id）。 */
    @Field("user_id")
    public String userId;
    /** 所屬公司（必填）；有專案時必等於專案的公司（需求書 12.8）。 */
    @Field("company_id")
    public String companyId;
    public String title;
    public String description;
    /** 所屬專案，null＝無專案；有分類時必等於分類的專案。 */
    @Field("project_id")
    public String projectId;
    @Field("category_id")
    public String categoryId;
    /** 開始時間，null＝已開始；時程表只列出該週結束前已開始的任務。 */
    @Field("start_at")
    public Instant startAt;
    public Instant deadline;
    /** 陣列順序即顯示順序。 */
    public List<Subtask> subtasks = new ArrayList<>();
    public boolean done;
    @Field("completed_at")
    public Instant completedAt;
    /** 手動封存時間；非 null 即封存。改回未完成時清除。 */
    @Field("archived_at")
    public Instant archivedAt;
    /** 最後一次解除封存時間；自動封存天數從 completed_at 與此欄較晚者起算（前端判斷，需求書 11）。 */
    @Field("unarchived_at")
    public Instant unarchivedAt;
    /** 非 null 即在回收桶。 */
    @Field("deleted_at")
    public Instant deletedAt;
    @CreatedDate
    @Field("created_at")
    public Instant createdAt;
    @LastModifiedDate
    @Field("updated_at")
    public Instant updatedAt;

    public static class Subtask {
        public String id;
        public String name;
        public boolean done;
    }
}
