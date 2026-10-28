package com.todoschedule.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 個人設定，每位使用者一筆，_id = 使用者 id（需求書 9.4、9.6）。
 * 摘要收件人固定為帳號的 Google Email，不另外存。
 */
@Document("settings")
public class Settings {

    @Id
    public String id;
    @Field("digest_enabled")
    public boolean digestEnabled;
    /** 每天寄送時間 HH:mm（台北時間）。 */
    @Field("digest_time")
    public String digestTime = "08:00";
    /** 最後處理摘要的日期 yyyy-MM-dd（台北時間），避免同一天重寄。 */
    @Field("last_digest_date")
    public String lastDigestDate;
    /** 最後一次摘要的結果，顯示在設定頁。 */
    @Field("last_digest_result")
    public String lastDigestResult;
}
