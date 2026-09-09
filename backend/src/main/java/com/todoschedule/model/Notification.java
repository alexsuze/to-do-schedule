package com.todoschedule.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * 通知排程與寄送紀錄：一筆 = 某事項在某時間要寄一封信。欄位定義見 Doc/MongoDB資料結構.md。
 */
@Document("notifications")
public class Notification {

    public static final String TYPE_REMINDER = "reminder";
    public static final String TYPE_DIGEST = "daily_digest";
    public static final String PENDING = "pending";
    public static final String SENT = "sent";
    public static final String FAILED = "failed";
    public static final String CANCELLED = "cancelled";

    @Id
    public String id;
    @Field("task_id")
    public String taskId;
    public String type = TYPE_REMINDER;
    @Field("lead_time")
    public String leadTime;
    /** 顯示用，紀錄頁不用再查事項。 */
    public String subject;
    @Field("scheduled_at")
    public Instant scheduledAt;
    @Field("sent_at")
    public Instant sentAt;
    public String status = PENDING;
    @Field("retry_count")
    public int retryCount;
    @Field("resend_message_id")
    public String resendMessageId;
    @Field("error_message")
    public String errorMessage;
    @CreatedDate
    @Field("created_at")
    public Instant createdAt;
}
