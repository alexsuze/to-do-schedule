package com.todoschedule.service;

import com.todoschedule.model.Task;
import com.todoschedule.model.Task.Subtask;
import com.todoschedule.repository.TaskRepository;
import com.todoschedule.service.TaskService.TaskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 驗證、完成時間、回收桶流程、只能動自己的任務。不需要 MongoDB。 */
class TaskServiceTest {

    private static final String ME = "u1";
    private static final String OTHER = "u2";

    private final TaskRepository repo = mock(TaskRepository.class);
    private final CategoryService categories = mock(CategoryService.class);
    private final TimeBlockService blocks = mock(TimeBlockService.class);
    private final TaskService service = new TaskService(repo, categories, blocks);
    private final List<Task> tasks = new ArrayList<>();

    TaskServiceTest() {
        when(repo.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            if (t.id == null) { t.id = "t" + (tasks.size() + 1); tasks.add(t); }
            return t;
        });
        when(repo.findById(anyString())).thenAnswer(inv ->
                tasks.stream().filter(t -> t.id.equals(inv.getArgument(0))).findFirst());
        when(categories.exists(ME, "c1")).thenReturn(true);
    }

    private static Subtask sub(String name) {
        Subtask s = new Subtask();
        s.name = name;
        return s;
    }

    private static TaskRequest req(String title, boolean done) {
        return new TaskRequest(title, null, null, null, null, done);
    }

    @Test
    void validatesAndCleansInput() {
        assertThrows(ResponseStatusException.class, () -> service.create(ME, req("  ", false)));
        assertThrows(ResponseStatusException.class, () -> service.create(ME, new TaskRequest("A", null, "nope", null, null, false)));
        assertThrows(ResponseStatusException.class, () -> service.create(OTHER, new TaskRequest("A", null, "c1", null, null, false)), "別人的分類");

        Task t = service.create(ME, new TaskRequest(" 報表 ", null, "c1", null, List.of(sub(" 匯出 "), sub(" ")), false));
        assertEquals(ME, t.userId);
        assertEquals("報表", t.title);
        assertEquals("c1", t.categoryId);
        assertEquals(1, t.subtasks.size());
        assertEquals("匯出", t.subtasks.get(0).name);
        assertNotNull(t.subtasks.get(0).id);
    }

    @Test
    void completedAtFollowsDone() {
        Task t = service.create(ME, req("A", false));
        assertNull(t.completedAt);
        service.update(ME, t.id, req("A", true));
        assertNotNull(t.completedAt);
        service.update(ME, t.id, req("A", false));
        assertNull(t.completedAt);
    }

    @Test
    void trashFlow() {
        Task t = service.create(ME, req("A", false));
        assertThrows(ResponseStatusException.class, () -> service.restore(ME, t.id));
        service.delete(ME, t.id);
        assertNotNull(t.deletedAt);
        assertThrows(ResponseStatusException.class, () -> service.update(ME, t.id, req("B", false)));
        service.restore(ME, t.id);
        assertNull(t.deletedAt);
    }

    @Test
    void cannotTouchOthersTasks() {
        Task t = service.create(ME, req("我的", false));
        assertThrows(ResponseStatusException.class, () -> service.update(OTHER, t.id, req("改掉", false)));
        assertThrows(ResponseStatusException.class, () -> service.delete(OTHER, t.id));
        assertEquals("我的", t.title);
        assertNull(t.deletedAt);
    }
}
