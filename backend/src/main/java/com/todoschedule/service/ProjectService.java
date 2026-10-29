package com.todoschedule.service;

import com.todoschedule.model.Category;
import com.todoschedule.model.Project;
import com.todoschedule.model.Task;
import com.todoschedule.repository.CategoryRepository;
import com.todoschedule.repository.ProjectRepository;
import com.todoschedule.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 專案管理（需求書 10.2）。只能操作自己的專案。
 */
@Service
public class ProjectService {

    private static final Pattern HEX = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final ProjectRepository repo;
    private final CategoryRepository categoryRepo;
    private final TaskRepository taskRepo;

    public ProjectService(ProjectRepository repo, CategoryRepository categoryRepo, TaskRepository taskRepo) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
        this.taskRepo = taskRepo;
    }

    /** archived 為 null 時不變更封存狀態。 */
    public record ProjectRequest(String name, String color, Boolean archived) {}

    public List<Project> list(String userId) {
        return repo.findByUserIdOrderByOrderAsc(userId);
    }

    public Project create(String userId, ProjectRequest req) {
        Project p = new Project();
        p.userId = userId;
        apply(p, req);
        p.order = list(userId).stream().mapToInt(x -> x.order + 1).max().orElse(0);
        return repo.save(p);
    }

    public Project update(String userId, String id, ProjectRequest req) {
        Project p = get(userId, id);
        apply(p, req);
        if (req.archived() != null) p.archived = req.archived();
        return repo.save(p);
    }

    /** 分類一併刪除；任務保留，改為無專案、未分類（工時歸到「無專案」）。 */
    public void delete(String userId, String id) {
        Project p = get(userId, id);
        List<Task> tasks = taskRepo.findByProjectId(p.id);
        tasks.forEach(t -> {
            t.projectId = null;
            t.categoryId = null;
        });
        taskRepo.saveAll(tasks);
        List<Category> categories = categoryRepo.findByProjectId(p.id);
        categoryRepo.deleteAll(categories);
        repo.delete(p);
    }

    /** ids 為新的完整順序。 */
    public List<Project> reorder(String userId, List<String> ids) {
        List<Project> all = list(userId);
        if (ids == null || ids.size() != all.size() || !ids.containsAll(all.stream().map(p -> p.id).toList())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "專案順序與現有專案不符，請重新整理");
        }
        all.forEach(p -> p.order = ids.indexOf(p.id));
        repo.saveAll(all);
        return list(userId);
    }

    /** 別人的專案一律當作找不到。 */
    public Project get(String userId, String id) {
        return repo.findById(id == null ? "" : id).filter(p -> userId.equals(p.userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "專案不存在，請重新選擇"));
    }

    private static void apply(Project p, ProjectRequest req) {
        String name = req.name() == null ? "" : req.name().trim();
        if (name.isEmpty() || name.length() > 30) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "專案名稱需為 1～30 字");
        if (req.color() == null || !HEX.matcher(req.color()).matches()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "顏色格式錯誤");
        p.name = name;
        p.color = req.color().toUpperCase();
    }
}
