package com.todoschedule.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 任務分類（統計頁依分類加總工時）。
 */
@Document("categories")
public class Category {

    @Id
    public String id;
    public String name;
    /** 例如 #8FD3FF */
    public String color;
    public int order;
}
