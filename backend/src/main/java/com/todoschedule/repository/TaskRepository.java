package com.todoschedule.repository;

import com.todoschedule.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {

    List<Task> findByBoardIdAndDeletedAtIsNullOrderByOrderAsc(String boardId);

    /** 總時程表用：全部未刪除事項。 */
    List<Task> findByDeletedAtIsNull();

    /** 完成超過一段時間、尚未刪除的事項（自動進回收桶用）。 */
    List<Task> findByCompletedAtBeforeAndDeletedAtIsNull(Instant before);

    List<Task> findByColumnIdAndDeletedAtIsNullOrderByOrderAsc(String columnId);

    /* 階段 */
    List<Task> findByParentIdAndDeletedAtIsNullOrderByStageOrderAsc(String parentId);

    List<Task> findByParentIdInAndDeletedAtIsNull(Collection<String> parentIds);

    /** 與所屬任務同時被刪的階段（deleted_at 相同）。 */
    List<Task> findByParentIdAndDeletedAt(String parentId, Instant deletedAt);

    void deleteByParentIdAndDeletedAtIsNotNull(String parentId);

    /* 回收桶 */
    List<Task> findByDeletedAtIsNotNullOrderByDeletedAtDesc();

    List<Task> findByBoardIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(String boardId);

    /** 與看板同時被刪的事項（deleted_at 相同）。 */
    List<Task> findByBoardIdAndDeletedAt(String boardId, Instant deletedAt);

    void deleteByDeletedAtBefore(Instant before);

    void deleteByDeletedAtIsNotNull();

    void deleteByBoardIdAndDeletedAtIsNotNull(String boardId);
}
