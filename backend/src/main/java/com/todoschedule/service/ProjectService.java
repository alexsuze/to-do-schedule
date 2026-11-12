package com.todoschedule.service;

import com.todoschedule.model.Category;
import com.todoschedule.model.Project;
import com.todoschedule.model.Task;
import com.todoschedule.model.TimeBlock;
import com.todoschedule.repository.CategoryRepository;
import com.todoschedule.repository.ProjectRepository;
import com.todoschedule.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 專案管理（需求書 10.2）。專案屬於某家公司，可移到其他公司（需求書 12.1）。只能操作自己的專案。
 */
@Service
public class ProjectService {

    private static final Pattern HEX = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final ProjectRepository repo;
    private final CategoryRepository categoryRepo;
    private final TaskRepository taskRepo;
    private final CompanyService companies;
    private final TimeBlockService blocks;

    public ProjectService(ProjectRepository repo, CategoryRepository categoryRepo, TaskRepository taskRepo,
                          CompanyService companies, TimeBlockService blocks) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
        this.taskRepo = taskRepo;
        this.companies = companies;
        this.blocks = blocks;
    }

    /** archived 為 null 時不變更封存狀態。companyId：新增時必填；編輯時給不同的公司即為「移到該公司」，null 表示不變。 */
    public record ProjectRequest(String name, String color, Boolean archived, String companyId) {}

    public List<Project> list(String userId) {
        return repo.findByUserIdOrderByOrderAsc(userId);
    }

    public Project create(String userId, ProjectRequest req) {
        Project p = new Project();
        p.userId = userId;
        p.companyId = companies.get(userId, req.companyId()).id;
        apply(p, req);
        p.order = list(userId).stream().mapToInt(x -> x.order + 1).max().orElse(0);
        return repo.save(p);
    }

    public Project update(String userId, String id, ProjectRequest req) {
        Project p = get(userId, id);
        apply(p, req);
        if (req.archived() != null) p.archived = req.archived();
        if (req.companyId() != null && !req.companyId().equals(p.companyId)) moveTo(p, companies.get(userId, req.companyId()).id);
        return repo.save(p);
    }

    /** 底下的任務（含回收桶）與時段跟著搬；有效任務的時段撞到新公司的時段時擋下。 */
    private void moveTo(Project p, String companyId) {
        List<Task> tasks = taskRepo.findByProjectId(p.id);
        List<String> active = tasks.stream().filter(t -> t.deletedAt == null).map(t -> t.id).toList();
        List<TimeBlock> clash = blocks.clashesIfMoved(p.userId, active, companyId);
        if (!clash.isEmpty()) throw TimeBlockService.clashError(clash);
        tasks.forEach(t -> t.companyId = companyId);
        taskRepo.saveAll(tasks);
        p.companyId = companyId;
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

    /** ids 為某家公司專案的新順序（設定頁只列目前公司）：沿用這些專案原本的排序位置重新分配，其他公司的專案不動。 */
    public List<Project> reorder(String userId, List<String> ids) {
        List<Project> all = list(userId);
        List<Project> picked = ids == null ? List.of() : ids.stream()
                .map(id -> all.stream().filter(p -> p.id.equals(id)).findFirst().orElse(null)).toList();
        if (picked.isEmpty() || picked.contains(null) || new HashSet<>(ids).size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "專案順序與現有專案不符，請重新整理");
        }
        for (int i = 0; i < all.size(); i++) all.get(i).order = i; // 先把舊資料可能重複的順序整理成連號
        List<Integer> slots = new ArrayList<>(picked.stream().map(p -> p.order).sorted().toList());
        for (int i = 0; i < picked.size(); i++) picked.get(i).order = slots.get(i);
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
