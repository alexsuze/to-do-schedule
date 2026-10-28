package com.todoschedule.repository;

import com.todoschedule.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {

    List<Task> findByDeletedAtIsNull();

    List<Task> findByCategoryId(String categoryId);

    /* 回收桶 */
    List<Task> findByDeletedAtIsNotNullOrderByDeletedAtDesc();

    List<Task> findByDeletedAtBefore(Instant before);
}
