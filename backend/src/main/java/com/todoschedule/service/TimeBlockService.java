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
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 時程表時段（需求書 3.2）：30 分鐘為單位、不可跨日、不可與自己同一家公司的其他時段重疊（不同公司可以，需求書 12.5）。
 * 回收桶中任務的時段不顯示、不佔用時間。只能操作自己的時段。
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

    /** 自己的有效時段（所屬任務不在回收桶）。ponytail: 一次回傳全部，資料量變大再加日期區間查詢。 */
    public List<TimeBlock> list(String userId) {
        Set<String> trashed = taskRepo.findByUserIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(userId).stream()
                .map(t -> t.id).collect(Collectors.toSet());
        return repo.findByUserId(userId).stream().filter(b -> !trashed.contains(b.taskId)).toList();
    }

    public TimeBlock create(String userId, BlockRequest req) {
        Task task = taskRepo.findById(req.taskId() == null ? "" : req.taskId())
                .filter(t -> userId.equals(t.userId) && t.deletedAt == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到此任務"));
        TimeBlock b = new TimeBlock();
        b.userId = userId;
        b.taskId = task.id;
        place(b, req.startAt(), req.endAt());
        return repo.save(b);
    }

    /** 移動或延展：只改起訖時間。 */
    public TimeBlock update(String userId, String id, BlockRequest req) {
        TimeBlock b = get(userId, id);
        place(b, req.startAt(), req.endAt());
        return repo.save(b);
    }

    public void delete(String userId, String id) {
        repo.delete(get(userId, id));
    }

    /** 任務還原後呼叫：刪除期間被其他任務佔用的時段不還原，回傳刪掉幾個。 */
    int dropConflicts(String userId, String taskId) {
        Map<String, String> company = companyOfTasks(userId);
        String mine = company.get(taskId);
        List<TimeBlock> others = list(userId).stream()
                .filter(b -> !b.taskId.equals(taskId) && Objects.equals(company.get(b.taskId), mine)).toList();
        List<TimeBlock> clash = repo.findByTaskId(taskId).stream()
                .filter(m -> others.stream().anyMatch(o -> overlaps(o, m.startAt, m.endAt)))
                .toList();
        repo.deleteAll(clash);
        return clash.size();
    }

    /**
     * 這些任務（taskIds）移到 companyId 時，會和該公司其他任務撞到的時段（專案或任務換公司前檢查，需求書 12.1）。
     */
    public List<TimeBlock> clashesIfMoved(String userId, Collection<String> taskIds, String companyId) {
        Map<String, String> company = companyOfTasks(userId);
        List<TimeBlock> all = list(userId);
        List<TimeBlock> others = all.stream()
                .filter(b -> !taskIds.contains(b.taskId) && Objects.equals(company.get(b.taskId), companyId)).toList();
        return all.stream().filter(b -> taskIds.contains(b.taskId))
                .filter(m -> others.stream().anyMatch(o -> overlaps(o, m.startAt, m.endAt)))
                .sorted((a, b) -> a.startAt.compareTo(b.startAt))
                .toList();
    }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("M/d");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final String WEEKDAYS = "一二三四五六日";

    /** 409：列出撞到的時段（最多 3 段）。 */
    public static ResponseStatusException clashError(List<TimeBlock> clash) {
        StringBuilder sb = new StringBuilder("會和那家公司已排的時段重疊：");
        for (int i = 0; i < Math.min(3, clash.size()); i++) {
            TimeBlock b = clash.get(i);
            var s = b.startAt.atZone(ZONE);
            if (i > 0) sb.append("、");
            sb.append(DAY.format(s)).append("（").append(WEEKDAYS.charAt(s.getDayOfWeek().getValue() - 1)).append("）")
                    .append(HM.format(s)).append("–").append(HM.format(b.endAt.atZone(ZONE)));
        }
        if (clash.size() > 3) sb.append(" 等 ").append(clash.size()).append(" 段");
        sb.append("。請先調整時段再搬移");
        return new ResponseStatusException(HttpStatus.CONFLICT, sb.toString());
    }

    void deleteForTasks(Collection<String> taskIds) {
        if (!taskIds.isEmpty()) repo.deleteByTaskIdIn(taskIds);
    }

    /** 別人的時段一律當作找不到。 */
    private TimeBlock get(String userId, String id) {
        return repo.findById(id).filter(b -> userId.equals(b.userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到此時段，請重新整理"));
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
        Map<String, String> company = companyOfTasks(b.userId);
        String mine = company.get(b.taskId);
        boolean clash = list(b.userId).stream().anyMatch(o -> !o.id.equals(b.id)
                && Objects.equals(company.get(o.taskId), mine) && overlaps(o, start, end));
        if (clash) throw new ResponseStatusException(HttpStatus.CONFLICT, "這段時間已經排了這家公司的其他任務");
        b.startAt = start;
        b.endAt = end;
    }

    /** 有效任務 id → 公司 id（值可能為 null：搬移前的舊資料）。 */
    private Map<String, String> companyOfTasks(String userId) {
        Map<String, String> m = new HashMap<>();
        taskRepo.findByUserIdAndDeletedAtIsNull(userId).forEach(t -> m.put(t.id, t.companyId));
        return m;
    }

    static boolean overlaps(TimeBlock o, Instant start, Instant end) {
        return o.startAt.isBefore(end) && o.endAt.isAfter(start);
    }
}
