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
 * ponytail: category_id 以 ObjectId 的 hex 字串儲存，單人系統不需 DBRef。
 */
@Document("tasks")
public class Task {

    @Id
    public String id;
    public String title;
    public String description;
    @Field("category_id")
    public String categoryId;
    public Instant deadline;
    /** 陣列順序即顯示順序。 */
    public List<Subtask> subtasks = new ArrayList<>();
    public boolean done;
    @Field("completed_at")
    public Instant completedAt;
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
