package com.trustid.notification.service;

import com.trustid.common.enums.NotificationType;
import com.trustid.notification.dto.NotificationResponse;
import com.trustid.notification.entity.InAppNotification;
import com.trustid.notification.repository.InAppNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class NotificationService {

    private final InAppNotificationRepository notificationRepository;

    @Transactional
    public void notifyUser(Long userId, NotificationType type, String title, String message, String metadataJson) {
        InAppNotification notification = InAppNotification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .metadataJson(metadataJson)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> myNotifications(Long userId) {
        return notificationRepository.findTop100ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(n -> NotificationResponse.builder()
                        .id(n.getId())
                        .type(n.getType())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .metadataJson(n.getMetadataJson())
                        .read(n.isRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        InAppNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized notification access");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }
}
