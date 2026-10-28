package com.todoschedule.service;

import com.todoschedule.model.Task;
import com.todoschedule.model.Task.Subtask;
import com.todoschedule.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 任務卡與回收桶（需求書 2.1、2.4）。
 */
@Service
public class TaskService {

    static final Duration TRASH_KEEP = Duration.ofDays(30);

    private final TaskRepository repo;
    private final CategoryService categories;
    private final TimeBlockService blocks;

    public TaskService(TaskRepository repo, CategoryService categories, TimeBlockService blocks) {
        this.repo = repo;
        this.categories = categories;
        this.blocks = blocks;
    }

    /** 新增與編輯共用，編輯為完整覆寫。 */
    public record TaskRequest(String title, String description, String categoryId, Instant deadline,
                              List<Subtask> subtasks, Boolean done) {}

    /** droppedBlocks：刪除期間被其他任務佔用、因此沒還原的時段數。 */
    public record RestoreResult(Task task, int droppedBlocks) {}

    public List<Task> list() {
        return repo.findByDeletedAtIsNull();
    }

    public Task create(TaskRequest req) {
        Task t = new Task();
        apply(t, req);
        return repo.save(t);
    }

    public Task update(String id, TaskRequest req) {
        Task t = get(id);
        if (t.deletedAt != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "任務在回收桶中，請先還原");
        apply(t, req);
        return repo.save(t);
    }

    public void delete(String id) {
        Task t = get(id);
        if (t.deletedAt != null) return;
        t.deletedAt = Instant.now();
        repo.save(t);
    }

    /* 回收桶 */

    public List<Task> trash() {
        return repo.findByDeletedAtIsNotNullOrderByDeletedAtDesc();
    }

    /** 時段隨任務一起回到時程表，被佔用的不還原。 */
    public RestoreResult restore(String id) {
        Task t = trashed(id);
        t.deletedAt = null;
        repo.save(t);
        return new RestoreResult(t, blocks.dropConflicts(t.id));
    }

    public void purge(String id) {
        purgeTasks(List.of(trashed(id)));
    }

    public void purgeAll() {
        purgeTasks(trash());
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Taipei")
    public void purgeExpired() {
        purgeTasks(repo.findByDeletedAtBefore(Instant.now().minus(TRASH_KEEP)));
    }

    /** 永久刪除任務與它的時段。 */
    private void purgeTasks(List<Task> tasks) {
        blocks.deleteForTasks(tasks.stream().map(t -> t.id).toList());
        repo.deleteAll(tasks);
    }

    private Task get(String id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到此任務"));
    }

    private Task trashed(String id) {
        Task t = get(id);
        if (t.deletedAt == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "此任務不在回收桶中");
        return t;
    }

    private void apply(Task t, TaskRequest req) {
        String title = req.title() == null ? "" : req.title().trim();
        if (title.isEmpty() || title.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "標題需為 1～100 字");
        String cat = req.categoryId() == null || req.categoryId().isBlank() ? null : req.categoryId();
        if (cat != null && !categories.exists(cat)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分類不存在，請重新選擇");

        t.title = title;
        t.description = req.description() == null ? "" : req.description();
        t.categoryId = cat;
        t.deadline = req.deadline();
        t.subtasks = new ArrayList<>();
        if (req.subtasks() != null) {
            for (Subtask s : req.subtasks()) {
                if (s == null || s.name == null || s.name.isBlank()) continue;
                s.name = s.name.trim();
                if (s.name.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "子任務名稱不可超過 100 字");
                if (s.id == null || s.id.isBlank()) s.id = UUID.randomUUID().toString();
                t.subtasks.add(s);
            }
        }
        boolean done = Boolean.TRUE.equals(req.done());
        if (done && !t.done) t.completedAt = Instant.now();
        if (!done) t.completedAt = null;
        t.done = done;
    }
}
