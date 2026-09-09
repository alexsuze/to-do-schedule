package com.todoschedule.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 看板，欄位（Column）內嵌其中。欄位定義見 Doc/MongoDB資料結構.md。
 * ponytail: 以 public 欄位取代 getter/setter，Spring Data 與 Jackson 皆直接存取欄位。
 */
@Document("boards")
@CompoundIndex(name = "deleted_at_order", def = "{'deleted_at': 1, 'order': 1}")
public class Board {

    @Id
    public String id;
    public String name;
    public String color;
    public int order;
    @Field("last_view_mode")
    public String lastViewMode = "kanban";
    public List<Column> columns = new ArrayList<>();
    @Field("deleted_at")
    public Instant deletedAt;
    @CreatedDate
    @Field("created_at")
    public Instant createdAt;
    @LastModifiedDate
    @Field("updated_at")
    public Instant updatedAt;

    public static class Column {
        @Id
        public String id;
        public String name;
        public int order;
        @Field("is_done_type")
        public boolean isDoneType;

        public Column() {}

        public Column(String id, String name, int order, boolean isDoneType) {
            this.id = id;
            this.name = name;
            this.order = order;
            this.isDoneType = isDoneType;
        }
    }
}
