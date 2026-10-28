package com.todoschedule.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 系統設定（單筆，id 固定為 {@link #SINGLETON_ID}）。需求書第 5 節。
 */
@Document("settings")
public class Settings {

    public static final String SINGLETON_ID = "app";

    @Id
    public String id = SINGLETON_ID;
    /** 摘要收件 Email（Resend 測試寄件者只能寄到 Resend 帳號的 Email）。 */
    public String email = "";
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
