package com.todoschedule.repository;

import com.todoschedule.model.Board;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BoardRepository extends MongoRepository<Board, String> {

    /** 未刪除的看板，依側邊欄順序。 */
    List<Board> findByDeletedAtIsNullOrderByOrderAsc();
}
