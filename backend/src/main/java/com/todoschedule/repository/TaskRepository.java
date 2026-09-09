package com.todoschedule.repository;

import com.todoschedule.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {

    List<Task> findByBoardIdAndDeletedAtIsNullOrderByOrderAsc(String boardId);

    List<Task> findByColumnIdAndDeletedAtIsNullOrderByOrderAsc(String columnId);
}
