package com.todoschedule.service;

import com.todoschedule.model.Board;
import com.todoschedule.model.Task;
import com.todoschedule.repository.BoardRepository;
import com.todoschedule.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 回收桶：列出 / 復原 / 永久刪除 / 30 天自動清理（需求書 3.1.4、3.1.5）。
 * ponytail: 復原後重建通知排程於階段 5 補上。
 */
@Service
public class TrashService {

    public static final Duration RETENTION = Duration.ofDays(30);
    private static final Logger log = LoggerFactory.getLogger(TrashService.class);

    /** 回收桶列表用，附上看板 / 欄位名稱與到期時間。 */
    public record TrashedTask(String id, String title, String boardId, String boardName, boolean boardDeleted,
                              String columnName, Instant deletedAt, Instant expiresAt) {}

    private final TaskRepository tasks;
    private final BoardRepository boards;

    public TrashService(TaskRepository tasks, BoardRepository boards) {
        this.tasks = tasks;
        this.boards = boards;
    }

    /** boardId 為 null 時列全部看板。 */
    public List<TrashedTask> listTasks(String boardId) {
        List<Task> list = boardId == null
                ? tasks.findByDeletedAtIsNotNullOrderByDeletedAtDesc()
                : tasks.findByBoardIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(boardId);
        Map<String, Board> boardMap = new HashMap<>();
        boards.findAllById(list.stream().map(t -> t.boardId).distinct().toList())
                .forEach(b -> boardMap.put(b.id, b));
        return list.stream().map(t -> {
            Board b = boardMap.get(t.boardId);
            String col = b == null ? "" : b.columns.stream()
                    .filter(c -> c.id.equals(t.columnId)).map(c -> c.name).findFirst().orElse("（欄位已刪除）");
            return new TrashedTask(t.id, t.title, t.boardId, b == null ? "（看板不存在）" : b.name,
                    b != null && b.deletedAt != null, col, t.deletedAt, t.deletedAt.plus(RETENTION));
        }).toList();
    }

    public List<Board> listBoards() {
        return boards.findByDeletedAtIsNotNullOrderByDeletedAtDesc();
    }

    /** 回到原欄位；原欄位不存在則回第一欄；看板已刪除則需先復原看板。 */
    public Task restoreTask(String id) {
        Task t = tasks.findById(id).filter(x -> x.deletedAt != null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "回收桶中沒有此事項"));
        Board b = boards.findById(t.boardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "所屬看板不存在，無法復原"));
        if (b.deletedAt != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "所屬看板「" + b.name + "」已刪除，請先復原看板");
        }
        restoreInto(t, b);
        return tasks.save(t);
    }

    /** 復原看板，並一併復原與看板同時被刪的事項。 */
    public Board restoreBoard(String id) {
        Board b = boards.findById(id).filter(x -> x.deletedAt != null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "回收桶中沒有此看板"));
        List<Task> together = tasks.findByBoardIdAndDeletedAt(b.id, b.deletedAt);
        b.deletedAt = null;
        b.order = boards.findByDeletedAtIsNullOrderByOrderAsc().stream().mapToInt(x -> x.order).max().orElse(-1) + 1;
        boards.save(b);
        for (Task t : together) restoreInto(t, b);
        tasks.saveAll(together);
        return b;
    }

    public void purgeTask(String id) {
        Task t = tasks.findById(id).filter(x -> x.deletedAt != null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "回收桶中沒有此事項"));
        tasks.delete(t);
    }

    public void purgeBoard(String id) {
        Board b = boards.findById(id).filter(x -> x.deletedAt != null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "回收桶中沒有此看板"));
        tasks.deleteByBoardIdAndDeletedAtIsNotNull(b.id);
        boards.delete(b);
    }

    /** 清空：boardId 為 null 時連已刪除的看板一起清。 */
    public void empty(String boardId) {
        if (boardId == null) {
            tasks.deleteByDeletedAtIsNotNull();
            boards.deleteByDeletedAtIsNotNull();
        } else {
            tasks.deleteByBoardIdAndDeletedAtIsNotNull(boardId);
        }
    }

    /** 每天 03:00（伺服器時區）永久刪除超過 30 天的資料。 */
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpired() {
        Instant before = Instant.now().minus(RETENTION);
        tasks.deleteByDeletedAtBefore(before);
        boards.deleteByDeletedAtBefore(before);
        log.info("回收桶清理完成：刪除 {} 之前的資料", before);
    }

    private void restoreInto(Task t, Board b) {
        boolean columnExists = b.columns.stream().anyMatch(c -> c.id.equals(t.columnId));
        if (!columnExists) t.columnId = b.columns.get(0).id;
        t.order = tasks.findByColumnIdAndDeletedAtIsNullOrderByOrderAsc(t.columnId).stream()
                .mapToInt(x -> x.order).max().orElse(-1) + 1;
        t.deletedAt = null;
    }
}
