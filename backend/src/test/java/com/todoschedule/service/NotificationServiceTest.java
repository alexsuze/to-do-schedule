package com.todoschedule.service;

import com.todoschedule.config.AppProperties;
import com.todoschedule.model.Board;
import com.todoschedule.model.Board.Column;
import com.todoschedule.model.Notification;
import com.todoschedule.model.Settings;
import com.todoschedule.model.Task;
import com.todoschedule.repository.BoardRepository;
import com.todoschedule.repository.NotificationRepository;
import com.todoschedule.repository.TaskRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 通知排程規則的最小檢查，不需要 MongoDB 與 Resend。 */
class NotificationServiceTest {

    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final BoardRepository boardRepo = mock(BoardRepository.class);
    private final SettingsService settingsService = mock(SettingsService.class);
    private final List<Notification> stored = new ArrayList<>();
    private final Settings settings = new Settings();
    private final Board board = new Board();
    private final NotificationService service = new NotificationService(repo, mock(TaskRepository.class), boardRepo,
            settingsService, mock(EmailService.class),
            new AppProperties("t", null, new AppProperties.Resend("", "x@y"), "http://f/"));

    NotificationServiceTest() {
        board.id = "b1";
        board.columns = List.of(Column.of("todo", "待辦", 0, false), Column.of("done", "完成", 1, true));
        when(boardRepo.findById("b1")).thenReturn(Optional.of(board));
        when(settingsService.get()).thenReturn(settings);
        when(repo.findByTaskId(anyString())).thenAnswer(inv ->
                stored.stream().filter(n -> n.taskId.equals(inv.getArgument(0))).toList());
        when(repo.saveAll(any())).thenAnswer(inv -> {
            for (Notification n : (Iterable<Notification>) inv.getArgument(0)) if (!stored.contains(n)) stored.add(n);
            return inv.getArgument(0);
        });
    }

    private Task task(String col, Instant due) {
        Task t = new Task();
        t.id = "t1"; t.boardId = "b1"; t.columnId = col; t.title = "x"; t.dueAt = due;
        return t;
    }

    private List<Notification> pending() {
        return stored.stream().filter(n -> Notification.PENDING.equals(n.status)).toList();
    }

    @Test
    void parseLeadTime() {
        assertEquals(Duration.ZERO, NotificationService.parseLeadTime("0m"));
        assertEquals(Duration.ofMinutes(15), NotificationService.parseLeadTime("15m"));
        assertEquals(Duration.ofHours(1), NotificationService.parseLeadTime("1h"));
        assertEquals(Duration.ofDays(7), NotificationService.parseLeadTime("1w"));
    }

    @Test
    void rescheduleUsesDefaultsThenOverrideAndCancelsOld() {
        Instant due = Instant.now().plus(Duration.ofDays(3));
        settings.defaultLeadTimes = List.of("1d", "1h");

        service.reschedule(task("todo", due));
        assertEquals(2, pending().size());
        assertEquals(due.minus(Duration.ofDays(1)), pending().get(0).scheduledAt);

        Task t = task("todo", due);
        t.notificationOverride = new Task.NotificationOverride();
        t.notificationOverride.leadTimes = List.of("15m");
        service.reschedule(t);
        assertEquals(1, pending().size());
        assertEquals("15m", pending().get(0).leadTime);
        assertEquals(2, stored.stream().filter(n -> Notification.CANCELLED.equals(n.status)).count());
    }

    @Test
    void meetingSchedulesFromStartAndIgnoresDue() {
        settings.defaultLeadTimes = List.of("1h");
        Task m = task("todo", Instant.now().plus(Duration.ofDays(2)));
        m.type = "meeting";
        assertTrue(!service.eligible(m)); // 會議沒有開始時間就不提醒
        m.startAt = Instant.now().plus(Duration.ofDays(1));
        service.reschedule(m);
        assertEquals(m.startAt.minus(Duration.ofHours(1)), pending().get(0).scheduledAt);
        assertTrue(pending().get(0).subject.startsWith("[會議提醒]"));
    }

    @Test
    void notEligibleWhenDoneOverdueDisabledOrNoDue() {
        Instant due = Instant.now().plus(Duration.ofDays(1));
        assertTrue(service.eligible(task("todo", due)));
        assertTrue(!service.eligible(task("done", due)));
        assertTrue(!service.eligible(task("todo", Instant.now().minus(Duration.ofMinutes(1)))));
        assertTrue(!service.eligible(task("todo", null)));
        Task off = task("todo", due);
        off.notificationOverride = new Task.NotificationOverride();
        off.notificationOverride.enabled = false;
        assertTrue(!service.eligible(off));
        Task completed = task("todo", due);
        completed.completedAt = Instant.now();
        assertTrue(!service.eligible(completed));
    }
}
