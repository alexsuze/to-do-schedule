package com.todoschedule.service;

import com.todoschedule.model.Board;
import com.todoschedule.model.Board.Column;
import com.todoschedule.model.Task;
import com.todoschedule.model.Task.ChecklistItem;
import com.todoschedule.model.Task.NotificationOverride;
import com.todoschedule.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 工作事項的業務規則（需求書 3.1、3.3.2）。變更後一律呼叫 notifications.reschedule 重算通知。
 */
@Service
public class TaskService {

    private static final List<String> PRIORITIES = List.of("high", "medium", "low");
    private static final List<String> TYPES = List.of("task", "meeting");

    /** 編輯視窗一次送完整欄位，null 即清空。 */
    public record TaskRequest(String title, String description, String priority, String type,
                              Instant startAt, Instant dueAt, List<String> tags,
                              NotificationOverride notificationOverride,
                              List<ChecklistItem> checklist) {}

    /** board_id 省略表示同看板；index 省略表示放到欄位最後。 */
    public record MoveRequest(String boardId, String columnId, Integer index) {}

    private final TaskRepository repo;
    private final BoardService boards;
    private final NotificationService notifications;

    public TaskService(TaskRepository repo, BoardService boards, NotificationService notifications) {
        this.repo = repo;
        this.boards = boards;
        this.notifications = notifications;
    }

    public List<Task> list(String boardId) {
        boards.get(boardId);
        return repo.findByBoardIdAndDeletedAtIsNullOrderByOrderAsc(boardId);
    }

    /** 全部未刪除看板的事項（總時程表）。 */
    public List<Task> listAll() {
        List<String> active = boards.list().stream().map(b -> b.id).toList();
        return repo.findByDeletedAtIsNull().stream().filter(t -> active.contains(t.boardId)).toList();
    }

    public Task get(String id) {
        return repo.findById(id)
                .filter(t -> t.deletedAt == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "事項不存在"));
    }

    /** 快速新增：只有標題，落在看板第一個欄位的最後。parentId 有值時建立為該任務的最後一個階段。 */
    public Task create(String boardId, String title, String parentId) {
        Board board = boards.get(boardId);
        Column first = firstColumn(board);
        Task t = new Task();
        t.boardId = board.id;
        t.columnId = first.id;
        t.title = BoardService.requireName(title, 100, "標題");
        if (parentId != null) {
            Task parent = get(parentId);
            if (parent.parentId != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "階段底下不能再分階段");
            }
            if (!parent.boardId.equals(board.id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "階段需與所屬任務在同一個看板");
            }
            t.parentId = parent.id;
            t.stageOrder = stages(parent.id).stream().mapToInt(x -> x.stageOrder).max().orElse(-1) + 1;
        }
        t.order = nextOrder(first.id);
        return repo.save(t);
    }

    /** 依傳入的 id 順序重排某任務的階段。 */
    public List<Task> reorderStages(String parentId, List<String> ids) {
        List<Task> list = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            Task s = get(ids.get(i));
            if (!parentId.equals(s.parentId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "「" + s.title + "」不是此任務的階段");
            }
            s.stageOrder = i;
            list.add(s);
        }
        repo.saveAll(list);
        return stages(parentId);
    }

    public Task update(String id, TaskRequest req) {
        Task t = get(id);
        t.title = BoardService.requireName(req.title(), 100, "標題");
        t.description = (req.description() == null || req.description().isBlank()) ? null : req.description();
        if (req.priority() != null) {
            if (!PRIORITIES.contains(req.priority())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "優先度不正確");
            }
            t.priority = req.priority();
        }
        if (req.type() != null) {
            if (!TYPES.contains(req.type())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "性質不正確");
            t.type = req.type();
        }
        if (t.isMeeting() && req.startAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "會議必須有開始時間");
        }
        validateDates(req.startAt(), req.dueAt());
        t.startAt = req.startAt();
        t.dueAt = req.dueAt();
        t.tags = req.tags() == null ? new ArrayList<>()
                : req.tags().stream().map(String::strip).filter(s -> !s.isEmpty()).distinct().toList();
        t.checklist = new ArrayList<>();
        for (ChecklistItem item : req.checklist() == null ? List.<ChecklistItem>of() : req.checklist()) {
            if (item.text == null || item.text.isBlank()) continue; // 空白項目直接略過
            ChecklistItem c = new ChecklistItem();
            c.text = BoardService.requireName(item.text, 100, "待辦項目");
            c.done = item.done;
            t.checklist.add(c);
        }
        if (req.notificationOverride() != null) {
            SettingsService.validLeadTimes(req.notificationOverride().leadTimes);
        }
        t.notificationOverride = req.notificationOverride();
        Task saved = repo.save(t);
        notifications.reschedule(saved);
        return saved;
    }

    /** 換欄位 / 換位置 / 跨看板；移入完成類型欄位填 completed_at，移出則清除。 */
    public Task move(String id, MoveRequest req) {
        Task t = get(id);
        Board board = boards.get(req.boardId() != null ? req.boardId() : t.boardId);
        boolean boardChanged = !board.id.equals(t.boardId);
        if (boardChanged && t.parentId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "階段需與所屬任務在同一個看板，請改移動所屬任務");
        }
        Column col = findColumn(board, req.columnId());

        List<Task> target = new ArrayList<>(repo.findByColumnIdAndDeletedAtIsNullOrderByOrderAsc(col.id));
        target.removeIf(x -> x.id.equals(t.id));
        int index = req.index() == null ? target.size() : Math.max(0, Math.min(req.index(), target.size()));
        target.add(index, t);

        // 換到別的欄位就重新計時（完成後 10 天自動進回收桶，見 TrashService）；同欄位內排序不重算
        boolean sameColumn = col.id.equals(t.columnId);
        t.boardId = board.id;
        t.columnId = col.id;
        t.completedAt = col.isDoneType ? (sameColumn && t.completedAt != null ? t.completedAt : Instant.now()) : null;
        for (int i = 0; i < target.size(); i++) target.get(i).order = i;
        repo.saveAll(target);
        notifications.reschedule(t);
        if (boardChanged) moveStages(t, board);
        return t;
    }

    /** 所屬任務換看板時階段一起搬：已完成的放第一個完成類型欄位，其餘放第一欄。 */
    private void moveStages(Task parent, Board board) {
        List<Task> list = stages(parent.id);
        Column first = firstColumn(board);
        Column done = board.columns.stream().filter(c -> c.isDoneType).findFirst().orElse(first);
        for (Task s : list) {
            Column col = s.completedAt != null ? done : first;
            s.boardId = board.id;
            s.columnId = col.id;
            s.order = nextOrder(col.id);
            s.completedAt = col.isDoneType ? (s.completedAt != null ? s.completedAt : Instant.now()) : null;
            repo.save(s);
        }
        notifications.rescheduleAll(list);
    }

    /** 刪除任務時，其階段一起進回收桶（deleted_at 相同，復原時一起回來）。 */
    public void delete(String id) {
        List<Task> list = withStages(repo, List.of(get(id)));
        Instant now = Instant.now();
        for (Task t : list) t.deletedAt = now;
        repo.saveAll(list);
        notifications.cancelAll(list);
    }

    /** 傳入的事項加上它們尚未刪除的階段（已在清單中的不重複加）。刪除 / 自動進回收桶共用。 */
    static List<Task> withStages(TaskRepository repo, List<Task> list) {
        List<String> parentIds = list.stream().filter(t -> t.parentId == null).map(t -> t.id).toList();
        if (parentIds.isEmpty()) return list;
        List<Task> all = new ArrayList<>(list);
        Set<String> seen = new HashSet<>(list.stream().map(t -> t.id).toList());
        for (Task s : repo.findByParentIdInAndDeletedAtIsNull(parentIds)) {
            if (seen.add(s.id)) all.add(s);
        }
        return all;
    }

    private List<Task> stages(String parentId) {
        return repo.findByParentIdAndDeletedAtIsNullOrderByStageOrderAsc(parentId);
    }

    private static Column firstColumn(Board board) {
        return board.columns.stream().min((a, b) -> Integer.compare(a.order, b.order))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "看板沒有任何欄位"));
    }

    static Column findColumn(Board board, String columnId) {
        return board.columns.stream().filter(c -> c.id.equals(columnId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "欄位不存在"));
    }

    static void validateDates(Instant start, Instant due) {
        if (start != null && due != null && due.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "到期時間不可早於開始時間");
        }
    }

    private int nextOrder(String columnId) {
        return repo.findByColumnIdAndDeletedAtIsNullOrderByOrderAsc(columnId).stream()
                .mapToInt(x -> x.order).max().orElse(-1) + 1;
    }
}
