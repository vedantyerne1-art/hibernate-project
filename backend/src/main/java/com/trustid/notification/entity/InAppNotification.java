package com.trustid.notification.entity;

import com.trustid.common.base.AuditableEntity;
import com.trustid.common.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "in_app_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InAppNotification extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    @Column(nullable = false)
    @Builder.Default
    private boolean read = false;

    @Version
    private Long version;
}
