package com.todoschedule.repository;

import com.todoschedule.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {

    List<Task> findByUserIdAndDeletedAtIsNull(String userId);

    List<Task> findByCategoryId(String categoryId);

    List<Task> findByProjectId(String projectId);

    /* 回收桶 */
    List<Task> findByUserIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(String userId);

    /** 30 天自動清除：不分使用者。 */
    List<Task> findByDeletedAtBefore(Instant before);
}
