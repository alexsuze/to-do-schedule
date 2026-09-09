package com.todoschedule.service;

import com.todoschedule.model.Board;
import com.todoschedule.model.Board.Column;
import com.todoschedule.model.Task;
import com.todoschedule.repository.BoardRepository;
import com.todoschedule.repository.TaskRepository;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 看板與欄位的業務規則（需求書 3.2、3.3.1）。
 */
@Service
public class BoardService {

    public static final String DEFAULT_BOARD_NAME = "我的工作";
    private static final List<String> VIEW_MODES = List.of("kanban", "timeline", "list");

    private final BoardRepository repo;
    private final TaskRepository tasks;

    public BoardService(BoardRepository repo, TaskRepository tasks) {
        this.repo = repo;
        this.tasks = tasks;
    }

    /** 列出未刪除看板；首次啟動沒有任何看板時自動建立預設看板。 */
    public List<Board> list() {
        List<Board> boards = repo.findByDeletedAtIsNullOrderByOrderAsc();
        return boards.isEmpty() ? List.of(create(DEFAULT_BOARD_NAME, null)) : boards;
    }

    public Board get(String id) {
        return repo.findById(id)
                .filter(b -> b.deletedAt == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "看板不存在"));
    }

    public Board create(String name, String color) {
        Board board = new Board();
        board.name = requireName(name, 50, "看板名稱");
        board.color = blankToNull(color);
        board.order = repo.findByDeletedAtIsNullOrderByOrderAsc().stream()
                .mapToInt(b -> b.order).max().orElse(-1) + 1;
        board.columns = new ArrayList<>(List.of(
                new Column(newId(), "待辦", 0, false),
                new Column(newId(), "進行中", 1, false),
                new Column(newId(), "完成", 2, true)));
        return repo.save(board);
    }

    /** 只更新有傳入（非 null）的欄位。 */
    public Board update(String id, String name, String color, String lastViewMode) {
        Board board = get(id);
        if (name != null) board.name = requireName(name, 50, "看板名稱");
        if (color != null) board.color = blankToNull(color);
        if (lastViewMode != null) {
            if (!VIEW_MODES.contains(lastViewMode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "檢視模式不正確");
            }
            board.lastViewMode = lastViewMode;
        }
        return repo.save(board);
    }

    /** 軟刪除看板與其下所有事項；至少保留一個看板。 */
    public void delete(String id) {
        Board board = get(id);
        if (repo.findByDeletedAtIsNullOrderByOrderAsc().size() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "至少需保留一個看板");
        }
        Instant now = Instant.now();
        board.deletedAt = now;
        repo.save(board);
        softDeleteTasks(tasks.findByBoardIdAndDeletedAtIsNullOrderByOrderAsc(id), now);
    }

    /** 依傳入的 id 順序重排側邊欄。 */
    public List<Board> reorder(List<String> ids) {
        List<Board> boards = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            Board b = get(ids.get(i));
            b.order = i;
            boards.add(b);
        }
        repo.saveAll(boards);
        return repo.findByDeletedAtIsNullOrderByOrderAsc();
    }

    /**
     * 整批覆寫欄位：沒有 id 的視為新增，順序依傳入陣列。
     * 被移除的欄位若仍有事項：removedTasks 為 "delete" 則一併軟刪除、為欄位 id 則搬過去、
     * 為 null 則回 409 讓前端詢問使用者。
     */
    public Board updateColumns(String id, List<Column> columns, String removedTasks) {
        if (columns == null || columns.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少需保留一個欄位");
        }
        Board board = get(id);
        List<Column> result = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            Column c = columns.get(i);
            String colId = (c.id == null || c.id.isBlank()) ? newId() : c.id;
            result.add(new Column(colId, requireName(c.name, 30, "欄位名稱"), i, c.isDoneType));
        }

        for (Column old : board.columns) {
            if (result.stream().anyMatch(c -> c.id.equals(old.id))) continue;
            List<Task> orphans = tasks.findByColumnIdAndDeletedAtIsNullOrderByOrderAsc(old.id);
            if (orphans.isEmpty()) continue;
            if (removedTasks == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "「" + old.name + "」內仍有 " + orphans.size() + " 個事項，請選擇一併刪除或移至其他欄位");
            }
            if ("delete".equals(removedTasks)) {
                softDeleteTasks(orphans, Instant.now());
            } else {
                Column target = result.stream().filter(c -> c.id.equals(removedTasks)).findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "目標欄位不存在"));
                int order = tasks.findByColumnIdAndDeletedAtIsNullOrderByOrderAsc(target.id).size();
                for (Task t : orphans) {
                    t.columnId = target.id;
                    t.order = order++;
                    t.completedAt = target.isDoneType ? (t.completedAt != null ? t.completedAt : Instant.now()) : null;
                }
                tasks.saveAll(orphans);
            }
        }

        board.columns = result;
        return repo.save(board);
    }

    /** ponytail: 取消通知於階段 5 補上。 */
    private void softDeleteTasks(List<Task> list, Instant at) {
        for (Task t : list) t.deletedAt = at;
        tasks.saveAll(list);
    }

    public static String requireName(String name, int max, String label) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "不可為空白");
        }
        String trimmed = name.strip();
        if (trimmed.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "不可超過 " + max + " 字");
        }
        return trimmed;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.strip();
    }

    private static String newId() {
        return new ObjectId().toHexString();
    }
}
