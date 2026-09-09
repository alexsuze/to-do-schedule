package com.todoschedule.service;

import com.todoschedule.model.Board;
import com.todoschedule.model.Task;
import com.todoschedule.repository.BoardRepository;
import com.todoschedule.repository.TaskRepository;
import com.todoschedule.service.TaskService.MoveRequest;
import com.todoschedule.service.TaskService.TaskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 事項排序、完成時間與驗證的最小檢查，不需要 MongoDB。 */
class TaskServiceTest {

    private final BoardRepository boardRepo = mock(BoardRepository.class);
    private final TaskRepository taskRepo = mock(TaskRepository.class);
    private final BoardService boards = new BoardService(boardRepo, taskRepo, mock(NotificationService.class));
    private final TaskService service = new TaskService(taskRepo, boards, mock(NotificationService.class));
    private final List<Task> tasks = new ArrayList<>();
    private final Board board;

    TaskServiceTest() {
        when(boardRepo.findByDeletedAtIsNullOrderByOrderAsc()).thenReturn(new ArrayList<>());
        when(boardRepo.save(any(Board.class))).thenAnswer(inv -> { Board b = inv.getArgument(0); b.id = "b1"; return b; });
        board = boards.create("A", null);
        when(boardRepo.findById("b1")).thenReturn(Optional.of(board));

        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            if (t.id == null) { t.id = "t" + (tasks.size() + 1); tasks.add(t); }
            return t;
        });
        when(taskRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepo.findById(anyString())).thenAnswer(inv ->
                tasks.stream().filter(t -> t.id.equals(inv.getArgument(0))).findFirst());
        when(taskRepo.findByColumnIdAndDeletedAtIsNullOrderByOrderAsc(anyString())).thenAnswer(inv ->
                tasks.stream().filter(t -> t.deletedAt == null && t.columnId.equals(inv.getArgument(0)))
                        .sorted((a, b) -> Integer.compare(a.order, b.order)).toList());
    }

    @Test
    void createGoesToFirstColumnAtEnd() {
        Task a = service.create("b1", "A");
        Task b = service.create("b1", "B");
        assertEquals(board.columns.get(0).id, a.columnId);
        assertEquals(0, a.order);
        assertEquals(1, b.order);
        assertThrows(ResponseStatusException.class, () -> service.create("b1", " "));
    }

    @Test
    void moveReordersAndSetsCompletedAt() {
        Task a = service.create("b1", "A");
        Task b = service.create("b1", "B");
        Task c = service.create("b1", "C");
        String todo = board.columns.get(0).id;
        String done = board.columns.get(2).id;

        service.move(c.id, new MoveRequest(null, todo, 0));
        assertEquals(0, c.order);
        assertEquals(1, a.order);
        assertEquals(2, b.order);

        service.move(a.id, new MoveRequest(null, done, null));
        assertEquals(done, a.columnId);
        assertNotNull(a.completedAt);
        assertEquals(0, a.order);

        service.move(a.id, new MoveRequest(null, todo, null));
        assertNull(a.completedAt);
        assertEquals(2, a.order);

        // 同欄位內排序不重算完成時間；離開再進入則重新計時
        service.move(a.id, new MoveRequest(null, done, null));
        Instant first = a.completedAt;
        service.move(a.id, new MoveRequest(null, done, 0));
        assertEquals(first, a.completedAt);
        service.move(a.id, new MoveRequest(null, todo, null));
        service.move(a.id, new MoveRequest(null, done, null));
        assertTrue(!a.completedAt.isBefore(first));

        assertThrows(ResponseStatusException.class, () -> service.move(a.id, new MoveRequest(null, "nope", null)));
    }

    @Test
    void updateValidatesDatesAndCleansTags() {
        Task a = service.create("b1", "A");
        Instant start = Instant.parse("2026-09-10T01:00:00Z");
        Instant before = Instant.parse("2026-09-10T00:30:00Z");
        assertThrows(ResponseStatusException.class, () ->
                service.update(a.id, new TaskRequest("A", null, "high", start, before, null, null, null)));

        Task u = service.update(a.id, new TaskRequest(" 標題 ", "  ", "low", start,
                Instant.parse("2026-09-10T15:00:00Z"), List.of(" x ", "x", "", "y"), null, null));
        assertEquals("標題", u.title);
        assertNull(u.description);
        assertEquals("low", u.priority);
        assertEquals(List.of("x", "y"), u.tags);
    }
}
