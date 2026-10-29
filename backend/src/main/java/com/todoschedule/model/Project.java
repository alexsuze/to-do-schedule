package com.todoschedule.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 專案（需求書第 10 節）：專案 → 分類 → 任務，工時以專案為統計主體。
 */
@Document("projects")
public class Project {

    @Id
    public String id;
    /** 擁有者（users 的 _id）。 */
    @Field("user_id")
    public String userId;
    public String name;
    /** 例如 #8FD3FF；時程表時段用這個顏色。 */
    public String color;
    public int order;
    /** 封存後不出現在選單，但工時保留、統計仍可查。 */
    public boolean archived;
}
