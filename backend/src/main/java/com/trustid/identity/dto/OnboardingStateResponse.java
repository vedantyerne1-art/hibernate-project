package com.trustid.identity.dto;

import com.trustid.common.enums.KycOnboardingStep;
import com.trustid.common.enums.IdentityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingStateResponse {
    private Long identityProfileId;
    private KycOnboardingStep currentStep;
    private Integer progress;
    private boolean completed;
    private IdentityStatus status;
    private String rejectionReason;
    private IdentityResponse profile;
}
