package com.todoschedule.service;

import com.todoschedule.model.Task;
import com.todoschedule.model.TimeBlock;
import com.todoschedule.repository.TaskRepository;
import com.todoschedule.repository.TimeBlockRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 時程表時段（需求書 3.2）：30 分鐘為單位、不可跨日、不可重疊。
 * 回收桶中任務的時段不顯示、不佔用時間。
 */
@Service
public class TimeBlockService {

    /** 判斷「同一天」用的時區（需求書第 5 節固定台北時間）。 */
    public static final ZoneId ZONE = ZoneId.of("Asia/Taipei");
    static final long SLOT_MS = Duration.ofMinutes(30).toMillis();

    private final TimeBlockRepository repo;
    private final TaskRepository taskRepo;

    public TimeBlockService(TimeBlockRepository repo, TaskRepository taskRepo) {
        this.repo = repo;
        this.taskRepo = taskRepo;
    }

    public record BlockRequest(String taskId, Instant startAt, Instant endAt) {}

    /** 有效時段（所屬任務不在回收桶）。ponytail: 一次回傳全部，資料量變大再加日期區間查詢。 */
    public List<TimeBlock> list() {
        Set<String> trashed = taskRepo.findByDeletedAtIsNotNullOrderByDeletedAtDesc().stream()
                .map(t -> t.id).collect(Collectors.toSet());
        return repo.findAll().stream().filter(b -> !trashed.contains(b.taskId)).toList();
    }

    public TimeBlock create(BlockRequest req) {
        Task task = taskRepo.findById(req.taskId() == null ? "" : req.taskId())
                .filter(t -> t.deletedAt == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到此任務"));
        TimeBlock b = new TimeBlock();
        b.taskId = task.id;
        place(b, req.startAt(), req.endAt());
        return repo.save(b);
    }

    /** 移動或延展：只改起訖時間。 */
    public TimeBlock update(String id, BlockRequest req) {
        TimeBlock b = get(id);
        place(b, req.startAt(), req.endAt());
        return repo.save(b);
    }

    public void delete(String id) {
        repo.delete(get(id));
    }

    /** 任務還原後呼叫：刪除期間被其他任務佔用的時段不還原，回傳刪掉幾個。 */
    int dropConflicts(String taskId) {
        List<TimeBlock> others = list().stream().filter(b -> !b.taskId.equals(taskId)).toList();
        List<TimeBlock> clash = repo.findByTaskId(taskId).stream()
                .filter(m -> others.stream().anyMatch(o -> overlaps(o, m.startAt, m.endAt)))
                .toList();
        repo.deleteAll(clash);
        return clash.size();
    }

    void deleteForTasks(Collection<String> taskIds) {
        if (!taskIds.isEmpty()) repo.deleteByTaskIdIn(taskIds);
    }

    private TimeBlock get(String id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到此時段，請重新整理"));
    }

    private void place(TimeBlock b, Instant start, Instant end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "時段時間不正確");
        }
        if (start.toEpochMilli() % SLOT_MS != 0 || end.toEpochMilli() % SLOT_MS != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "時段需以 30 分鐘為單位");
        }
        if (!start.atZone(ZONE).toLocalDate().equals(end.minusMillis(1).atZone(ZONE).toLocalDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "時段不可跨日");
        }
        boolean clash = list().stream().anyMatch(o -> !o.id.equals(b.id) && overlaps(o, start, end));
        if (clash) throw new ResponseStatusException(HttpStatus.CONFLICT, "這段時間已經排了其他任務");
        b.startAt = start;
        b.endAt = end;
    }

    static boolean overlaps(TimeBlock o, Instant start, Instant end) {
        return o.startAt.isBefore(end) && o.endAt.isAfter(start);
    }
}
