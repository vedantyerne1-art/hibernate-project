package com.trustid.identity.entity;

import com.trustid.common.base.AuditableEntity;
import com.trustid.common.enums.KycOnboardingStep;
import com.trustid.common.enums.IdentityStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "identity_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentityProfile extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String identityNumber;

    @Column(nullable = false)
    private String fullName;

    private LocalDate dob;
    private String gender;

    private String fatherName;
    private String motherName;
    private String occupation;
    private String maritalStatus;

    private String phone;
    private String alternatePhone;
    
    @Column(columnDefinition = "TEXT")
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

    @Column(nullable = false)
    @Builder.Default
    private boolean permanentSameAsCurrent = true;

    private String permanentAddressLine1;
    private String permanentAddressLine2;
    private String permanentCity;
    private String permanentDistrict;
    private String permanentState;
    private String permanentPincode;
    private String permanentCountry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdentityStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KycOnboardingStep onboardingStep = KycOnboardingStep.STEP_1_PERSONAL;

    @Column(nullable = false)
    @Builder.Default
    private boolean onboardingCompleted = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer onboardingProgress = 0;

    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(nullable = false)
    private Long userId;

    @Version
    private Long version;
}
