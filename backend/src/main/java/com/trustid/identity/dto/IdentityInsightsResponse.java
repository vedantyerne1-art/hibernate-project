package com.trustid.identity.dto;

import com.trustid.common.enums.IdentityLevel;
import com.trustid.common.enums.RiskLevel;
import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IdentityInsightsResponse {
    private Integer trustScore;
    private IdentityLevel identityLevel;
    private RiskLevel riskLevel;
    private List<String> suggestions;
    private List<String> alerts;
}
