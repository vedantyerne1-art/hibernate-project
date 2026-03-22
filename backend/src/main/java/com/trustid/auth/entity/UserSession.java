package com.trustid.auth.entity;

import com.trustid.common.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String sessionToken;

    private String deviceName;
    private String ipAddress;
    private String location;

    @Column(length = 1000)
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime lastActiveAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Version
    private Long version;
}
