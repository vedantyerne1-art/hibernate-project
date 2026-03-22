package com.trustid.notification.repository;

import com.trustid.notification.entity.InAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {
    List<InAppNotification> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadFalse(Long userId);
}
