package com.todoschedule.repository;

import com.todoschedule.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {

    List<Task> findByBoardIdAndDeletedAtIsNullOrderByOrderAsc(String boardId);

    /** 總時程表用：全部未刪除事項。 */
    List<Task> findByDeletedAtIsNull();

    /** 完成超過一段時間、尚未刪除的事項（自動進回收桶用）。 */
    List<Task> findByCompletedAtBeforeAndDeletedAtIsNull(Instant before);

    List<Task> findByColumnIdAndDeletedAtIsNullOrderByOrderAsc(String columnId);

    /* 回收桶 */
    List<Task> findByDeletedAtIsNotNullOrderByDeletedAtDesc();

    List<Task> findByBoardIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(String boardId);

    /** 與看板同時被刪的事項（deleted_at 相同）。 */
    List<Task> findByBoardIdAndDeletedAt(String boardId, Instant deletedAt);

    void deleteByDeletedAtBefore(Instant before);

    void deleteByDeletedAtIsNotNull();

    void deleteByBoardIdAndDeletedAtIsNotNull(String boardId);
}
