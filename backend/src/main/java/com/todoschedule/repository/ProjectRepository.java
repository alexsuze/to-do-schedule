package com.todoschedule.repository;

import com.todoschedule.model.Project;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProjectRepository extends MongoRepository<Project, String> {

    List<Project> findByUserIdOrderByOrderAsc(String userId);
}
