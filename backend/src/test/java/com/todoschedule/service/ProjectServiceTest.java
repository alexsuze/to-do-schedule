package com.todoschedule.service;

import com.todoschedule.model.Category;
import com.todoschedule.model.Project;
import com.todoschedule.model.Task;
import com.todoschedule.repository.CategoryRepository;
import com.todoschedule.repository.ProjectRepository;
import com.todoschedule.repository.TaskRepository;
import com.todoschedule.service.CategoryService.CategoryRequest;
import com.todoschedule.service.ProjectService.ProjectRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 專案：建立 / 封存 / 刪除連動、分類必須屬於自己的專案、移動分類時任務跟著走。不需要 MongoDB。 */
class ProjectServiceTest {

    private static final String ME = "u1";
    private static final String OTHER = "u2";

    private final ProjectRepository projectRepo = mock(ProjectRepository.class);
    private final CategoryRepository categoryRepo = mock(CategoryRepository.class);
    private final TaskRepository taskRepo = mock(TaskRepository.class);
    private final ProjectService projects = new ProjectService(projectRepo, categoryRepo, taskRepo);
    private final CategoryService categories = new CategoryService(categoryRepo, taskRepo, projects);

    private final List<Project> ps = new ArrayList<>();
    private final List<Category> cs = new ArrayList<>();
    private final List<Task> ts = new ArrayList<>();

    ProjectServiceTest() {
        when(projectRepo.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            if (p.id == null) { p.id = "p" + (ps.size() + 1); ps.add(p); }
            return p;
        });
        when(projectRepo.findById(anyString())).thenAnswer(inv -> ps.stream().filter(p -> p.id.equals(inv.getArgument(0))).findFirst());
        when(projectRepo.findByUserIdOrderByOrderAsc(anyString())).thenAnswer(inv -> ps.stream().filter(p -> p.userId.equals(inv.getArgument(0))).toList());
        doAnswer(inv -> ps.remove((Project) inv.getArgument(0))).when(projectRepo).delete(any(Project.class));

        when(categoryRepo.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            if (c.id == null) { c.id = "c" + (cs.size() + 1); cs.add(c); }
            return c;
        });
        when(categoryRepo.findById(anyString())).thenAnswer(inv -> cs.stream().filter(c -> c.id.equals(inv.getArgument(0))).findFirst());
        when(categoryRepo.findByUserIdOrderByOrderAsc(anyString())).thenAnswer(inv -> cs.stream().filter(c -> c.userId.equals(inv.getArgument(0))).toList());
        when(categoryRepo.findByProjectId(anyString())).thenAnswer(inv -> cs.stream().filter(c -> inv.getArgument(0).equals(c.projectId)).toList());
        doAnswer(inv -> { cs.removeAll((Collection<?>) inv.getArgument(0)); return null; }).when(categoryRepo).deleteAll(any());

        when(taskRepo.findByCategoryId(anyString())).thenAnswer(inv -> ts.stream().filter(t -> inv.getArgument(0).equals(t.categoryId)).toList());
        when(taskRepo.findByProjectId(anyString())).thenAnswer(inv -> ts.stream().filter(t -> inv.getArgument(0).equals(t.projectId)).toList());
    }

    private Task task(String projectId, String categoryId) {
        Task t = new Task();
        t.userId = ME;
        t.projectId = projectId;
        t.categoryId = categoryId;
        ts.add(t);
        return t;
    }

    @Test
    void createValidateAndArchive() {
        assertThrows(ResponseStatusException.class, () -> projects.create(ME, new ProjectRequest(" ", "#FFFFFF", null)));
        assertThrows(ResponseStatusException.class, () -> projects.create(ME, new ProjectRequest("樂天", "red", null)));
        Project p = projects.create(ME, new ProjectRequest(" 樂天 ", "#8fd3ff", null));
        assertEquals("樂天", p.name);
        assertEquals("#8FD3FF", p.color);

        projects.update(ME, p.id, new ProjectRequest("樂天", "#8FD3FF", true));
        assertTrue(p.archived);
        projects.update(ME, p.id, new ProjectRequest("樂天", "#8FD3FF", null)); // null 不改封存狀態
        assertTrue(p.archived);
        assertThrows(ResponseStatusException.class, () -> projects.update(OTHER, p.id, new ProjectRequest("改", "#8FD3FF", null)));
    }

    @Test
    void categoryMustBelongToOwnProjectAndMoveCarriesTasks() {
        Project a = projects.create(ME, new ProjectRequest("A", "#8FD3FF", null));
        Project b = projects.create(ME, new ProjectRequest("B", "#8EE3B8", null));
        Project theirs = projects.create(OTHER, new ProjectRequest("X", "#8EE3B8", null));

        assertThrows(ResponseStatusException.class, () -> categories.create(ME, new CategoryRequest("KM", "#FFD966", null)), "沒選專案");
        assertThrows(ResponseStatusException.class, () -> categories.create(ME, new CategoryRequest("KM", "#FFD966", theirs.id)), "別人的專案");

        Category km = categories.create(ME, new CategoryRequest("KM", "#FFD966", a.id));
        Task t = task(a.id, km.id);
        categories.update(ME, km.id, new CategoryRequest("KM", "#FFD966", b.id));
        assertEquals(b.id, km.projectId);
        assertEquals(b.id, t.projectId, "任務跟著分類移到新專案");
    }

    @Test
    void deleteProjectKeepsTasksAsNoProject() {
        Project a = projects.create(ME, new ProjectRequest("A", "#8FD3FF", null));
        Category km = categories.create(ME, new CategoryRequest("KM", "#FFD966", a.id));
        Task withCat = task(a.id, km.id);
        Task noCat = task(a.id, null);

        projects.delete(ME, a.id);
        assertTrue(ps.isEmpty());
        assertTrue(cs.isEmpty(), "分類一併刪除");
        assertEquals(2, ts.size(), "任務保留");
        assertNull(withCat.projectId);
        assertNull(withCat.categoryId);
        assertNull(noCat.projectId);
    }
}
