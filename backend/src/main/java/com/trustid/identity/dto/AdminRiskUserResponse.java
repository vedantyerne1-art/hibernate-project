package com.trustid.identity.dto;

import com.trustid.common.enums.IdentityLevel;
import com.trustid.common.enums.IdentityStatus;
import com.trustid.common.enums.RiskLevel;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminRiskUserResponse {
    private Long userId;
    private String fullName;
    private String email;
    private Integer trustScore;
    private IdentityLevel identityLevel;
    private IdentityStatus status;
    private RiskLevel riskLevel;
}
