package com.trustid.identity.dto;

import com.trustid.common.enums.KycOnboardingStep;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingStepRequest {
    @NotNull
    private KycOnboardingStep step;

    @Builder.Default
    private boolean saveAsDraft = true;

    @Valid
    private IdentityCreateRequest payload;
}
