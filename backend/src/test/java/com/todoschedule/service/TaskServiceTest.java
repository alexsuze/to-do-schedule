package com.todoschedule.service;

import com.todoschedule.model.Category;
import com.todoschedule.model.Project;
import com.todoschedule.model.Task;
import com.todoschedule.model.Task.Subtask;
import com.todoschedule.repository.TaskRepository;
import com.todoschedule.service.TaskService.TaskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.time.Instant;
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

/** 驗證、完成時間、回收桶流程、只能動自己的任務。不需要 MongoDB。 */
class TaskServiceTest {

    private static final String ME = "u1";
    private static final String OTHER = "u2";

    private final TaskRepository repo = mock(TaskRepository.class);
    private final CategoryService categories = mock(CategoryService.class);
    private final ProjectService projects = mock(ProjectService.class);
    private final TimeBlockService blocks = mock(TimeBlockService.class);
    private final TaskService service = new TaskService(repo, categories, projects, blocks);
    private final List<Task> tasks = new ArrayList<>();

    TaskServiceTest() {
        when(repo.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            if (t.id == null) { t.id = "t" + (tasks.size() + 1); tasks.add(t); }
            return t;
        });
        when(repo.findById(anyString())).thenAnswer(inv ->
                tasks.stream().filter(t -> t.id.equals(inv.getArgument(0))).findFirst());
        Category c1 = new Category();
        c1.id = "c1";
        c1.projectId = "p1";
        when(categories.find(anyString(), anyString())).thenReturn(Optional.empty());
        when(categories.find(ME, "c1")).thenReturn(Optional.of(c1));
        Project p2 = new Project();
        p2.id = "p2";
        when(projects.get(ME, "p2")).thenReturn(p2);
        when(projects.get(OTHER, "p2")).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST));
    }

    private static Subtask sub(String name) {
        Subtask s = new Subtask();
        s.name = name;
        return s;
    }

    private static TaskRequest req(String title, boolean done) {
        return new TaskRequest(title, null, null, null, null, null, null, done);
    }

    @Test
    void validatesAndCleansInput() {
        assertThrows(ResponseStatusException.class, () -> service.create(ME, req("  ", false)));
        assertThrows(ResponseStatusException.class, () -> service.create(ME, new TaskRequest("A", null, null, "nope", null, null, null, false)));
        assertThrows(ResponseStatusException.class, () -> service.create(OTHER, new TaskRequest("A", null, null, "c1", null, null, null, false)), "別人的分類");
        assertThrows(ResponseStatusException.class, () -> service.create(OTHER, new TaskRequest("A", null, "p2", null, null, null, null, false)), "別人的專案");

        // 有分類時專案以分類的專案為準（傳了不一樣的專案也會被修正）
        Task t = service.create(ME, new TaskRequest(" 報表 ", null, "p2", "c1", null, null, List.of(sub(" 匯出 "), sub(" ")), false));
        assertEquals(ME, t.userId);
        assertEquals("報表", t.title);
        assertEquals("c1", t.categoryId);
        assertEquals("p1", t.projectId);
        assertEquals(1, t.subtasks.size());
        assertEquals("匯出", t.subtasks.get(0).name);
        assertNotNull(t.subtasks.get(0).id);
    }

    @Test
    void startMustNotBeAfterDeadline() {
        Instant d = Instant.parse("2026-11-05T10:00:00Z");
        assertThrows(ResponseStatusException.class, () -> service.create(ME, new TaskRequest("A", null, null, null, d.plusSeconds(60), d, null, false)));
        assertEquals(d, service.create(ME, new TaskRequest("A", null, null, null, d, d, null, false)).startAt);
    }

    @Test
    void projectWithoutCategoryOrNone() {
        assertEquals("p2", service.create(ME, new TaskRequest("A", null, "p2", null, null, null, null, false)).projectId);
        assertNull(service.create(ME, req("B", false)).projectId, "無專案");
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
