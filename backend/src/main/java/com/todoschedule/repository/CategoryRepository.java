package com.todoschedule.repository;

import com.todoschedule.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CategoryRepository extends MongoRepository<Category, String> {

    List<Category> findByUserIdOrderByOrderAsc(String userId);

    List<Category> findByProjectId(String projectId);
}
