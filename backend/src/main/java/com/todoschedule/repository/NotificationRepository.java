package com.todoschedule.repository;

import com.todoschedule.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByTaskId(String taskId);

    List<Notification> findByTaskIdInAndStatus(List<String> taskIds, String status);

    /** 排程器每分鐘掃描用。 */
    List<Notification> findByStatusAndScheduledAtLessThanEqual(String status, Instant before);

    boolean existsByTypeAndScheduledAt(String type, Instant scheduledAt);

    List<Notification> findTop50ByOrderByCreatedAtDesc();
}
