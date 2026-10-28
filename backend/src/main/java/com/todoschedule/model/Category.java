package com.todoschedule.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 任務分類（統計頁依分類加總工時）。
 */
@Document("categories")
public class Category {

    @Id
    public String id;
    /** 擁有者（users 的 _id）。 */
    @Field("user_id")
    public String userId;
    public String name;
    /** 例如 #8FD3FF */
    public String color;
    public int order;
}
