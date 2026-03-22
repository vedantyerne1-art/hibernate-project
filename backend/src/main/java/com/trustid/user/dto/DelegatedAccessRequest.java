package com.trustid.user.dto;

import com.trustid.common.enums.DelegationPermission;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DelegatedAccessRequest {
    private Long delegateUserId;
    private DelegationPermission permission;
    private LocalDateTime expiresAt;
}
