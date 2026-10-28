package com.todoschedule.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * 時程表上的一個時段（需求書 3.2）。工時 = end_at - start_at。
 */
@Document("time_blocks")
public class TimeBlock {

    @Id
    public String id;
    @Field("task_id")
    public String taskId;
    /** 對齊 30 分鐘。 */
    @Field("start_at")
    public Instant startAt;
    /** 對齊 30 分鐘，與 start_at 同一天（台北時間）。 */
    @Field("end_at")
    public Instant endAt;
}
