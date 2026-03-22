package com.trustid.notification.dto;

import com.trustid.common.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private String metadataJson;
    private boolean read;
    private LocalDateTime createdAt;
}
