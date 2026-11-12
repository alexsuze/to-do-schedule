package com.todoschedule.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 公司（需求書第 12 節）：公司 → 專案 → 分類 → 任務。只是自己分類用，不與他人共用。
 */
@Document("companies")
public class Company {

    @Id
    public String id;
    /** 擁有者（users 的 _id）。 */
    @Field("user_id")
    public String userId;
    public String name;
    /** 例如 #FFC49E；用在切換按鈕的方塊。 */
    public String color;
    public int order;
    /** 封存後不出現在切換選單，資料保留。至少要留一家未封存。 */
    public boolean archived;
}
