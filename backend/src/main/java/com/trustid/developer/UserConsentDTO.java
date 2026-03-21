package com.trustid.developer;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserConsentDTO {
    private Long id;
    private String clientName;
    private String organizationName;
    private String scopes;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private boolean isRevoked;
}
