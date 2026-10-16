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
 * 工作事項。欄位定義見 Doc/MongoDB資料結構.md。
 * ponytail: board_id / column_id 以 ObjectId 的 hex 字串儲存，單人系統不需 DBRef。
 */
@Document("tasks")
public class Task {

    @Id
    public String id;
    @Field("board_id")
    public String boardId;
    @Field("column_id")
    public String columnId;
    public String title;
    public String description;
    public String priority = "medium";
    /** 性質：task（任務，提醒以到期時間為準）/ meeting（會議，提醒以開始時間為準）。舊資料無此欄位視為 task。 */
    public String type = "task";
    @Field("start_at")
    public Instant startAt;
    @Field("due_at")
    public Instant dueAt;
    public int order;
    /** 所屬任務 id；有值即為「階段」卡片，只允許一層，且與所屬任務同看板。 */
    @Field("parent_id")
    public String parentId;
    /** 階段在所屬任務內的順序（與欄位內的 order 無關）。 */
    @Field("stage_order")
    public int stageOrder;
    public List<String> tags = new ArrayList<>();
    /** 事項內的待辦清單（checkbox + 名稱）。 */
    public List<ChecklistItem> checklist = new ArrayList<>();
    /** null 表示沿用全域設定；設定介面於階段 5 實作。 */
    @Field("notification_override")
    public NotificationOverride notificationOverride;
    @Field("completed_at")
    public Instant completedAt;
    @Field("deleted_at")
    public Instant deletedAt;
    @CreatedDate
    @Field("created_at")
    public Instant createdAt;
    @LastModifiedDate
    @Field("updated_at")
    public Instant updatedAt;

    public boolean isMeeting() { return "meeting".equals(type); }

    /** 提醒與顯示的基準時間：會議看開始，任務看到期。 */
    public Instant anchorAt() { return isMeeting() ? startAt : dueAt; }

    public static class ChecklistItem {
        public String text;
        public boolean done;
    }

    public static class NotificationOverride {
        public boolean enabled = true;
        @Field("lead_times")
        public List<String> leadTimes = new ArrayList<>();
    }
}
