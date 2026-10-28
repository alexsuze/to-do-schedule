package com.todoschedule.service;

import com.todoschedule.model.Category;
import com.todoschedule.model.Task;
import com.todoschedule.repository.CategoryRepository;
import com.todoschedule.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 分類管理（需求書 2.3）。刪除分類時任務改為未分類，不刪任務。只能操作自己的分類。
 */
@Service
public class CategoryService {

    private static final Pattern HEX = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final CategoryRepository repo;
    private final TaskRepository taskRepo;

    public CategoryService(CategoryRepository repo, TaskRepository taskRepo) {
        this.repo = repo;
        this.taskRepo = taskRepo;
    }

    public record CategoryRequest(String name, String color) {}

    public List<Category> list(String userId) {
        return repo.findByUserIdOrderByOrderAsc(userId);
    }

    public Category create(String userId, CategoryRequest req) {
        Category c = new Category();
        c.userId = userId;
        apply(c, req);
        c.order = list(userId).stream().mapToInt(x -> x.order + 1).max().orElse(0);
        return repo.save(c);
    }

    public Category update(String userId, String id, CategoryRequest req) {
        Category c = get(userId, id);
        apply(c, req);
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

    public boolean exists(String userId, String id) {
        return repo.findById(id).filter(c -> userId.equals(c.userId)).isPresent();
    }

    /** 別人的分類一律當作找不到。 */
    private Category get(String userId, String id) {
        return repo.findById(id).filter(c -> userId.equals(c.userId))
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
