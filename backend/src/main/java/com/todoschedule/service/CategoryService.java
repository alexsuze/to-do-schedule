package com.todoschedule.service;

import com.todoschedule.model.Category;
import com.todoschedule.model.Task;
import com.todoschedule.repository.CategoryRepository;
import com.todoschedule.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 分類管理（需求書 2.3、10.2）。分類屬於某個專案，可移到其他專案（任務跟著移）。
 * 刪除分類時任務改為未分類（專案不變），不刪任務。只能操作自己的分類。
 */
@Service
public class CategoryService {

    private static final Pattern HEX = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final CategoryRepository repo;
    private final TaskRepository taskRepo;
    private final ProjectService projects;

    public CategoryService(CategoryRepository repo, TaskRepository taskRepo, ProjectService projects) {
        this.repo = repo;
        this.taskRepo = taskRepo;
        this.projects = projects;
    }

    /** projectId：新增時必填；編輯時給不同的專案即為「移到該專案」，null 表示不變。 */
    public record CategoryRequest(String name, String color, String projectId) {}

    public List<Category> list(String userId) {
        return repo.findByUserIdOrderByOrderAsc(userId);
    }

    public Category create(String userId, CategoryRequest req) {
        Category c = new Category();
        c.userId = userId;
        c.projectId = projects.get(userId, req.projectId()).id;
        apply(c, req);
        c.order = list(userId).stream().mapToInt(x -> x.order + 1).max().orElse(0);
        return repo.save(c);
    }

    public Category update(String userId, String id, CategoryRequest req) {
        Category c = get(userId, id);
        apply(c, req);
        if (req.projectId() != null && !req.projectId().equals(c.projectId)) {
            c.projectId = projects.get(userId, req.projectId()).id;
            // 任務跟著分類移到新專案
            List<Task> tasks = taskRepo.findByCategoryId(c.id);
            tasks.forEach(t -> t.projectId = c.projectId);
            taskRepo.saveAll(tasks);
        }
        return repo.save(c);
    }

    public void delete(String userId, String id) {
        get(userId, id);
        List<Task> tasks = taskRepo.findByCategoryId(id);
        tasks.forEach(t -> t.categoryId = null);
        taskRepo.saveAll(tasks);
        repo.deleteById(id);
    }

    /** ids 為新的完整順序。 */
    public List<Category> reorder(String userId, List<String> ids) {
        List<Category> all = list(userId);
        if (ids == null || ids.size() != all.size() || !ids.containsAll(all.stream().map(c -> c.id).toList())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分類順序與現有分類不符，請重新整理");
        }
        all.forEach(c -> c.order = ids.indexOf(c.id));
        repo.saveAll(all);
        return list(userId);
    }

    /** 自己的分類；別人的或不存在回傳 empty。 */
    public Optional<Category> find(String userId, String id) {
        return repo.findById(id).filter(c -> userId.equals(c.userId));
    }

    /** 別人的分類一律當作找不到。 */
    private Category get(String userId, String id) {
        return find(userId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到此分類"));
    }

    private static void apply(Category c, CategoryRequest req) {
        String name = req.name() == null ? "" : req.name().trim();
        if (name.isEmpty() || name.length() > 30) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分類名稱需為 1～30 字");
        if (req.color() == null || !HEX.matcher(req.color()).matches()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "顏色格式錯誤");
        c.name = name;
        c.color = req.color().toUpperCase();
    }
}
