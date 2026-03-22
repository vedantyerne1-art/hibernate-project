package com.trustid.user.entity;

import com.trustid.common.base.AuditableEntity;
import com.trustid.common.enums.DelegationPermission;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "delegated_access")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DelegatedAccess extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerUserId;

    @Column(nullable = false)
    private Long delegateUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DelegationPermission permission;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    private LocalDateTime expiresAt;

    @Version
    private Long version;
}
