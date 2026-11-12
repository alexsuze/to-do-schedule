package com.todoschedule.repository;

import com.todoschedule.model.Company;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CompanyRepository extends MongoRepository<Company, String> {

    List<Company> findByUserIdOrderByOrderAsc(String userId);
}
