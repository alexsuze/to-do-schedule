package com.todoschedule.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 全域設定，永遠只有一筆（_id = "global"）。欄位定義見 Doc/MongoDB資料結構.md。
 */
@Document("settings")
public class Settings {

    public static final String ID = "global";
    /** 可用的提前時間代碼。 */
    public static final List<String> LEAD_TIMES = List.of("0m", "15m", "1h", "1d", "3d", "1w");

    @Id
    public String id = ID;
    @Field("notify_email")
    public String notifyEmail;
    @Field("notifications_enabled")
    public boolean notificationsEnabled = true;
    @Field("default_lead_times")
    public List<String> defaultLeadTimes = new ArrayList<>(List.of("1d"));
    @Field("daily_digest_enabled")
    public boolean dailyDigestEnabled = false;
    @Field("daily_digest_time")
    public String dailyDigestTime = "09:00";
    public String timezone = "Asia/Taipei";
    @Field("last_board_id")
    public String lastBoardId;
    @LastModifiedDate
    @Field("updated_at")
    public Instant updatedAt;
}
