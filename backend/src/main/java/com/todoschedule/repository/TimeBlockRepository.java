package com.todoschedule.repository;

import com.todoschedule.model.TimeBlock;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface TimeBlockRepository extends MongoRepository<TimeBlock, String> {

    List<TimeBlock> findByUserId(String userId);

    List<TimeBlock> findByTaskId(String taskId);

    void deleteByTaskIdIn(Collection<String> taskIds);
}
