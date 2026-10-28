package com.todoschedule.service;

import com.todoschedule.config.AppProperties;
import com.todoschedule.model.Settings;
import com.todoschedule.model.Task;
import com.todoschedule.model.TimeBlock;
import com.todoschedule.repository.TaskRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.todoschedule.service.TimeBlockService.ZONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 摘要內容分類、空的不寄、同一天只處理一次、未到時間不寄。不需要 MongoDB。 */
class DigestServiceTest {

    private final SettingsService settings = mock(SettingsService.class);
    private final TaskRepository taskRepo = mock(TaskRepository.class);
    private final TimeBlockService blocks = mock(TimeBlockService.class);
    private final EmailService email = mock(EmailService.class);
    private final DigestService service = new DigestService(settings, taskRepo, blocks, email,
            new AppProperties("t", null, null, "https://example.test"));
    private final List<Task> tasks = new ArrayList<>();
    private final Settings s = new Settings();

    DigestServiceTest() {
        when(taskRepo.findByDeletedAtIsNull()).thenReturn(tasks);
        when(blocks.list()).thenReturn(List.of());
        when(settings.get()).thenReturn(s);
        s.email = "me@example.test";
        s.digestEnabled = true;
    }

    /** 台北時間 2026-10-28 的 h:m（day 為相對天數） */
    private static ZonedDateTime at(int day, int h, int m) {
        return LocalDateTime.of(2026, 10, 28, h, m).plusDays(day).atZone(ZONE);
    }

    private static Task task(String id, String title, ZonedDateTime deadline, boolean done) {
        Task t = new Task();
        t.id = id;
        t.title = title;
        t.deadline = deadline == null ? null : deadline.toInstant();
        t.done = done;
        return t;
    }

    private static TimeBlock block(String taskId, ZonedDateTime s, ZonedDateTime e) {
        TimeBlock b = new TimeBlock();
        b.taskId = taskId;
        b.startAt = s.toInstant();
        b.endAt = e.toInstant();
        return b;
    }

    @Test
    void buildsSections() {
        tasks.add(task("a", "今天下午到期", at(0, 18, 0), false));
        tasks.add(task("b", "昨天到期", at(-1, 18, 0), false));
        tasks.add(task("c", "已完成不列", at(-1, 18, 0), true));
        tasks.add(task("d", "明天到期不列", at(1, 9, 0), false));
        tasks.add(task("e", "排程任務", null, false));
        when(blocks.list()).thenReturn(List.of(
                block("e", at(0, 13, 0), at(0, 14, 30)),
                block("a", at(0, 9, 0), at(0, 10, 0)),
                block("e", at(1, 9, 0), at(1, 10, 0)), // 明天：不列、不算
                block("e", at(-2, 9, 0), at(-2, 11, 30)), // 週一：算本週工時
                block("a", at(-1, 14, 0), at(-1, 15, 0)), // 週二：算本週工時
                block("e", at(-3, 9, 0), at(-3, 10, 0)))); // 上週日：不算

        DigestService.Digest d = service.build(at(0, 8, 0).toInstant());
        assertEquals(List.of("18:00 今天下午到期"), d.dueToday());
        assertEquals(List.of("10/27 18:00 昨天到期"), d.overdue());
        assertEquals(List.of("09:00–10:00 今天下午到期", "13:00–14:30 排程任務"), d.schedule());
        assertEquals("[每日摘要] 10/28（三） 今日到期 1 項、逾期 1 項、排程 2 段", service.subject(d));
        // 週三寄：只算週一 2.5h + 週二 1h，今天的 2.5h 不算
        assertEquals("本週累計工時 3.5h（10/26–10/27）", service.weekLine(d));
    }

    @Test
    void weekLineHiddenWithoutPastHours() {
        tasks.add(task("a", "今天到期", at(0, 18, 0), false));
        when(blocks.list()).thenReturn(List.of(block("a", at(-2, 9, 0), at(-2, 10, 0))));
        assertNull(service.weekLine(service.build(at(-2, 8, 0).toInstant())), "週一寄：沒有已過去的天數");
        assertEquals("本週累計工時 1h（10/26–10/26）", service.weekLine(service.build(at(-1, 8, 0).toInstant())));
        when(blocks.list()).thenReturn(List.of());
        assertNull(service.weekLine(service.build(at(0, 8, 0).toInstant())), "本週沒有工時");
    }

    @Test
    void sendsOncePerDayAfterTime() {
        tasks.add(task("a", "今天到期", at(0, 18, 0), false));

        service.run(at(0, 7, 59));
        verify(email, never()).send(anyString(), anyString(), anyString(), anyString());

        service.run(at(0, 8, 0));
        service.run(at(0, 8, 1));
        verify(email, times(1)).send(anyString(), anyString(), anyString(), anyString());
        assertEquals("2026-10-28", s.lastDigestDate);
        assertTrue(s.lastDigestResult.endsWith("已寄出"));
    }

    @Test
    void emptyDigestIsNotSent() {
        service.run(at(0, 9, 0));
        verify(email, never()).send(anyString(), anyString(), anyString(), anyString());
        verify(settings).save(any());
        assertTrue(s.lastDigestResult.contains("未寄送"));
    }
}
