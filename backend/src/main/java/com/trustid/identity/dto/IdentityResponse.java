package com.trustid.identity.dto;

import com.trustid.common.enums.IdentityStatus;
import com.trustid.common.enums.IdentityLevel;
import com.trustid.common.enums.KycOnboardingStep;
import com.trustid.common.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IdentityResponse {
    private Long id;
    private String identityNumber;
    private String fullName;
    private LocalDate dob;
    private String gender;
    private String fatherName;
    private String motherName;
    private String occupation;
    private String maritalStatus;
    private String phone;
    private String alternatePhone;
    private String address;
    private String nationality;
    private String profilePhotoUrl;
    private String currentAddressLine1;
    private String currentAddressLine2;
    private String currentCity;
    private String currentDistrict;
    private String currentState;
    private String currentPincode;
    private String currentCountry;
    private boolean permanentSameAsCurrent;
    private String permanentAddressLine1;
    private String permanentAddressLine2;
    private String permanentCity;
    private String permanentDistrict;
    private String permanentState;
    private String permanentPincode;
    private String permanentCountry;
    private KycOnboardingStep onboardingStep;
    private Integer onboardingProgress;
    private boolean onboardingCompleted;
    private IdentityStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private Integer trustScore;
    private IdentityLevel identityLevel;
    private RiskLevel riskLevel;
}
