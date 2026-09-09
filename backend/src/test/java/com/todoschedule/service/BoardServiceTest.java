package com.todoschedule.service;

import com.todoschedule.model.Board;
import com.todoschedule.model.Board.Column;
import com.todoschedule.model.Task;
import com.todoschedule.repository.BoardRepository;
import com.todoschedule.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 看板業務規則的最小檢查，不需要 MongoDB。 */
class BoardServiceTest {

    private final BoardRepository repo = mock(BoardRepository.class);
    private final TaskRepository taskRepo = mock(TaskRepository.class);
    private final BoardService service = new BoardService(repo, taskRepo);
    private final List<Board> active = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();

    BoardServiceTest() {
        when(taskRepo.findByBoardIdAndDeletedAtIsNullOrderByOrderAsc(anyString())).thenAnswer(inv ->
                tasks.stream().filter(t -> t.deletedAt == null && t.boardId.equals(inv.getArgument(0))).toList());
        when(taskRepo.findByColumnIdAndDeletedAtIsNullOrderByOrderAsc(anyString())).thenAnswer(inv ->
                tasks.stream().filter(t -> t.deletedAt == null && t.columnId.equals(inv.getArgument(0))).toList());
        when(repo.findByDeletedAtIsNullOrderByOrderAsc()).thenReturn(active);
        when(repo.save(any(Board.class))).thenAnswer(inv -> {
            Board b = inv.getArgument(0);
            if (b.id == null) {
                b.id = "id" + (active.size() + 1);
                active.add(b);
            }
            return b;
        });
        when(repo.findById(anyString())).thenAnswer(inv ->
                active.stream().filter(b -> b.id.equals(inv.getArgument(0))).findFirst());
    }

    @Test
    void listCreatesDefaultBoardWithThreeColumns() {
        List<Board> boards = service.list();
        assertEquals(1, boards.size());
        Board b = boards.get(0);
        assertEquals("我的工作", b.name);
        assertEquals(3, b.columns.size());
        assertTrue(b.columns.get(2).isDoneType);
        assertNotNull(b.columns.get(0).id);
    }

    @Test
    void createAppendsOrderAndRejectsBadName() {
        service.create("A", null);
        Board second = service.create("  B  ", "");
        assertEquals(1, second.order);
        assertEquals("B", second.name);
        assertEquals(null, second.color);
        assertThrows(ResponseStatusException.class, () -> service.create("   ", null));
        assertThrows(ResponseStatusException.class, () -> service.create("x".repeat(51), null));
    }

    @Test
    void cannotDeleteLastBoard() {
        Board only = service.create("A", null);
        assertThrows(ResponseStatusException.class, () -> service.delete(only.id));
        Board other = service.create("B", null);
        service.delete(other.id);
        assertNotNull(other.deletedAt);
        assertThrows(ResponseStatusException.class, () -> service.get(other.id));
    }

    @Test
    void updateColumnsAssignsIdsAndOrder() {
        Board b = service.create("A", null);
        String keepId = b.columns.get(0).id;
        Column kept = new Column(keepId, "待辦改名", 9, false);
        Column added = new Column(null, "新欄", 0, true);
        Board updated = service.updateColumns(b.id, List.of(added, kept), null);
        assertEquals(2, updated.columns.size());
        assertEquals(0, updated.columns.get(0).order);
        assertNotNull(updated.columns.get(0).id);
        assertEquals(keepId, updated.columns.get(1).id);
        assertEquals(1, updated.columns.get(1).order);
        assertThrows(ResponseStatusException.class, () -> service.updateColumns(b.id, List.of(), null));
    }

    @Test
    void removingColumnWithTasksNeedsDecision() {
        Board b = service.create("A", null);
        Column todo = b.columns.get(0);
        Column doing = b.columns.get(1);
        Task t = new Task();
        t.id = "t1"; t.boardId = b.id; t.columnId = todo.id; t.title = "x";
        tasks.add(t);

        List<Column> without = List.of(doing, b.columns.get(2));
        assertThrows(ResponseStatusException.class, () -> service.updateColumns(b.id, without, null));

        service.updateColumns(b.id, without, doing.id);
        assertEquals(doing.id, t.columnId);
        assertEquals(0, t.order);

        service.create("B", null);
        service.delete(b.id);
        assertNotNull(t.deletedAt);
    }
}
