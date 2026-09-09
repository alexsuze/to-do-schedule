package com.todoschedule.service;

import com.todoschedule.model.Board;
import com.todoschedule.model.Board.Column;
import com.todoschedule.model.Task;
import com.todoschedule.repository.BoardRepository;
import com.todoschedule.repository.TaskRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 復原規則的最小檢查，不需要 MongoDB。 */
class TrashServiceTest {

    private final BoardRepository boardRepo = mock(BoardRepository.class);
    private final TaskRepository taskRepo = mock(TaskRepository.class);
    private final TrashService service = new TrashService(taskRepo, boardRepo, mock(NotificationService.class));
    private final List<Board> allBoards = new ArrayList<>();
    private final List<Task> allTasks = new ArrayList<>();

    private final Board board = new Board();
    private final Instant boardDeletedAt = Instant.parse("2026-09-01T00:00:00Z");

    TrashServiceTest() {
        board.id = "b1";
        board.name = "B";
        board.columns = List.of(Column.of("c1", "待辦", 0, false), Column.of("c2", "完成", 1, true));
        allBoards.add(board);

        when(boardRepo.findById(anyString())).thenAnswer(inv ->
                allBoards.stream().filter(b -> b.id.equals(inv.getArgument(0))).findFirst());
        when(boardRepo.findByDeletedAtIsNullOrderByOrderAsc()).thenAnswer(inv ->
                allBoards.stream().filter(b -> b.deletedAt == null).toList());
        when(boardRepo.save(any(Board.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepo.findById(anyString())).thenAnswer(inv ->
                allTasks.stream().filter(t -> t.id.equals(inv.getArgument(0))).findFirst());
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepo.findByColumnIdAndDeletedAtIsNullOrderByOrderAsc(anyString())).thenAnswer(inv ->
                allTasks.stream().filter(t -> t.deletedAt == null && t.columnId.equals(inv.getArgument(0))).toList());
        when(taskRepo.findByBoardIdAndDeletedAt(anyString(), any())).thenAnswer(inv ->
                allTasks.stream().filter(t -> t.boardId.equals(inv.getArgument(0)) && inv.getArgument(1).equals(t.deletedAt)).toList());
    }

    private Task deleted(String id, String columnId, Instant at) {
        Task t = new Task();
        t.id = id; t.boardId = "b1"; t.columnId = columnId; t.title = id; t.deletedAt = at;
        allTasks.add(t);
        return t;
    }

    @Test
    void restoreGoesBackToColumnOrFirstColumnWhenMissing() {
        Task live = new Task();
        live.id = "live"; live.boardId = "b1"; live.columnId = "c1"; live.order = 4;
        allTasks.add(live);

        Task a = deleted("a", "c2", Instant.now());
        Task b = deleted("b", "gone", Instant.now());

        service.restoreTask("a");
        assertNull(a.deletedAt);
        assertEquals("c2", a.columnId);

        service.restoreTask("b");
        assertEquals("c1", b.columnId);
        assertEquals(5, b.order);

        assertThrows(ResponseStatusException.class, () -> service.restoreTask("live"));
        assertThrows(ResponseStatusException.class, () -> service.restoreTask("nope"));
    }

    @Test
    void restoreBlockedWhenBoardDeletedAndBoardRestoreBringsItsTasks() {
        board.deletedAt = boardDeletedAt;
        Task withBoard = deleted("x", "c1", boardDeletedAt);
        Task earlier = deleted("y", "c1", Instant.parse("2026-08-20T00:00:00Z"));

        assertThrows(ResponseStatusException.class, () -> service.restoreTask("x"));

        service.restoreBoard("b1");
        assertNull(board.deletedAt);
        assertNull(withBoard.deletedAt);
        assertNotNull(earlier.deletedAt);
    }
}
