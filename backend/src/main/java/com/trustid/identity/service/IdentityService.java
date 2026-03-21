package com.trustid.identity.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.trustid.common.enums.IdentityStatus;
import com.trustid.common.enums.KycOnboardingStep;
import com.trustid.document.service.FileStorageService;
import com.trustid.identity.dto.IdentityCreateRequest;
import com.trustid.identity.dto.IdentityResponse;
import com.trustid.identity.dto.OnboardingStateResponse;
import com.trustid.identity.dto.OnboardingStepRequest;
import com.trustid.identity.entity.IdentityProfile;
import com.trustid.identity.repository.IdentityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class IdentityService {

    private final IdentityRepository identityRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public IdentityResponse createOrUpdateIdentity(Long userId, IdentityCreateRequest request) {
        IdentityProfile profile = identityRepository.findByUserId(userId)
                .orElse(new IdentityProfile());

        mergeIdentity(profile, userId, request);
        
        if (profile.getId() == null) {
            profile.setStatus(IdentityStatus.DRAFT);
        } else if (profile.getStatus() == IdentityStatus.REJECTED) {
            // Rejected users can edit and resubmit their profile details.
            profile.setStatus(IdentityStatus.DRAFT);
            profile.setRejectedAt(null);
            profile.setRejectionReason(null);
        }

        IdentityProfile saved = identityRepository.save(profile);
        return mapToResponse(saved);
    }

    @Transactional
    public OnboardingStateResponse saveOnboardingStep(Long userId, OnboardingStepRequest stepRequest) {
        IdentityProfile profile = identityRepository.findByUserId(userId).orElse(new IdentityProfile());
        mergeIdentity(profile, userId, stepRequest.getPayload());

        if (!stepRequest.isSaveAsDraft()) {
            validateStep(stepRequest.getStep(), profile);
        }

        profile.setOnboardingStep(stepRequest.getStep());
        profile.setOnboardingProgress(Math.max(progressFor(stepRequest.getStep()), profile.getOnboardingProgress() == null ? 0 : profile.getOnboardingProgress()));
        profile.setOnboardingCompleted(stepRequest.getStep() == KycOnboardingStep.STEP_6_REVIEW_SUBMIT && !stepRequest.isSaveAsDraft());

        if (profile.getStatus() == null) {
            profile.setStatus(IdentityStatus.DRAFT);
        }

        IdentityProfile saved = identityRepository.save(profile);
        return mapToOnboardingState(saved);
    }

    @Transactional(readOnly = true)
    public OnboardingStateResponse getOnboardingState(Long userId) {
        IdentityProfile profile = identityRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            return OnboardingStateResponse.builder()
                    .identityProfileId(null)
                    .currentStep(KycOnboardingStep.STEP_1_PERSONAL)
                    .progress(0)
                    .completed(false)
                    .status(IdentityStatus.DRAFT)
                    .build();
        }
        return mapToOnboardingState(profile);
    }

    @Transactional
    public OnboardingStateResponse submitOnboarding(Long userId) {
        IdentityProfile profile = identityRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Identity profile not found"));

        validateStep(KycOnboardingStep.STEP_1_PERSONAL, profile);
        validateStep(KycOnboardingStep.STEP_2_CONTACT, profile);
        validateStep(KycOnboardingStep.STEP_3_ADDRESS, profile);
        validateStep(KycOnboardingStep.STEP_4_PROFILE_PHOTO, profile);

        profile.setOnboardingStep(KycOnboardingStep.STEP_6_REVIEW_SUBMIT);
        profile.setOnboardingProgress(100);
        profile.setOnboardingCompleted(true);
        if (profile.getStatus() == IdentityStatus.REJECTED || profile.getStatus() == IdentityStatus.RESUBMISSION_REQUIRED) {
            profile.setRejectionReason(null);
            profile.setRejectedAt(null);
        }
        profile.setStatus(IdentityStatus.PENDING);

        return mapToOnboardingState(identityRepository.save(profile));
    }

    @Transactional
    public IdentityResponse uploadProfilePhoto(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Profile photo is required");
        }

        String incomingContentType = file.getContentType();
        String contentType = incomingContentType == null ? "" : incomingContentType.toLowerCase();
        if (!(contentType.contains("jpeg") || contentType.contains("jpg") || contentType.contains("png") || contentType.contains("webp"))) {
            throw new RuntimeException("Only image files are allowed for profile photo");
        }

        IdentityProfile profile = identityRepository.findByUserId(userId).orElse(new IdentityProfile());
        String fileName = fileStorageService.storeFile(file, userId);

        profile.setUserId(userId);
        profile.setProfilePhotoUrl(fileName);
        profile.setOnboardingStep(KycOnboardingStep.STEP_4_PROFILE_PHOTO);
        profile.setOnboardingProgress(Math.max(66, profile.getOnboardingProgress() == null ? 0 : profile.getOnboardingProgress()));
        if (profile.getStatus() == null) {
            profile.setStatus(IdentityStatus.DRAFT);
        }

        return mapToResponse(identityRepository.save(profile));
    }

    public IdentityResponse getIdentity(Long userId) {
        IdentityProfile profile = identityRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Identity profile not found"));
        return mapToResponse(profile);
    }

    private IdentityResponse mapToResponse(IdentityProfile profile) {
        return IdentityResponse.builder()
                .id(profile.getId())
                .identityNumber(profile.getIdentityNumber())
                .fullName(profile.getFullName())
                .dob(profile.getDob())
                .gender(profile.getGender())
                .fatherName(profile.getFatherName())
                .motherName(profile.getMotherName())
                .occupation(profile.getOccupation())
                .maritalStatus(profile.getMaritalStatus())
                .phone(profile.getPhone())
                .alternatePhone(profile.getAlternatePhone())
                .address(profile.getAddress())
                .nationality(profile.getNationality())
                .status(profile.getStatus())
                .profilePhotoUrl(profile.getProfilePhotoUrl())
                .currentAddressLine1(profile.getCurrentAddressLine1())
                .currentAddressLine2(profile.getCurrentAddressLine2())
                .currentCity(profile.getCurrentCity())
                .currentDistrict(profile.getCurrentDistrict())
                .currentState(profile.getCurrentState())
                .currentPincode(profile.getCurrentPincode())
                .currentCountry(profile.getCurrentCountry())
                .permanentSameAsCurrent(profile.isPermanentSameAsCurrent())
                .permanentAddressLine1(profile.getPermanentAddressLine1())
                .permanentAddressLine2(profile.getPermanentAddressLine2())
                .permanentCity(profile.getPermanentCity())
                .permanentDistrict(profile.getPermanentDistrict())
                .permanentState(profile.getPermanentState())
                .permanentPincode(profile.getPermanentPincode())
                .permanentCountry(profile.getPermanentCountry())
                .onboardingStep(profile.getOnboardingStep())
                .onboardingProgress(profile.getOnboardingProgress())
                .onboardingCompleted(profile.isOnboardingCompleted())
                .submittedAt(profile.getSubmittedAt())
                .approvedAt(profile.getApprovedAt())
                .rejectedAt(profile.getRejectedAt())
                .rejectionReason(profile.getRejectionReason())
                .build();
    }

    private OnboardingStateResponse mapToOnboardingState(IdentityProfile profile) {
        return OnboardingStateResponse.builder()
                .identityProfileId(profile.getId())
                .currentStep(profile.getOnboardingStep())
                .progress(profile.getOnboardingProgress())
                .completed(profile.isOnboardingCompleted())
                .status(profile.getStatus())
                .rejectionReason(profile.getRejectionReason())
                .profile(mapToResponse(profile))
                .build();
    }

    private void mergeIdentity(IdentityProfile profile, Long userId, IdentityCreateRequest request) {
        if (request == null) {
            return;
        }
        profile.setUserId(userId);
        if (request.getFullName() != null) profile.setFullName(request.getFullName());
        if (request.getDob() != null) profile.setDob(request.getDob());
        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getAddress() != null) profile.setAddress(request.getAddress());
        if (request.getNationality() != null) profile.setNationality(request.getNationality());
        if (request.getFatherName() != null) profile.setFatherName(request.getFatherName());
        if (request.getMotherName() != null) profile.setMotherName(request.getMotherName());
        if (request.getOccupation() != null) profile.setOccupation(request.getOccupation());
        if (request.getMaritalStatus() != null) profile.setMaritalStatus(request.getMaritalStatus());
        if (request.getPhone() != null) profile.setPhone(request.getPhone());
        if (request.getAlternatePhone() != null) profile.setAlternatePhone(request.getAlternatePhone());
        if (request.getCurrentAddressLine1() != null) profile.setCurrentAddressLine1(request.getCurrentAddressLine1());
        if (request.getCurrentAddressLine2() != null) profile.setCurrentAddressLine2(request.getCurrentAddressLine2());
        if (request.getCurrentCity() != null) profile.setCurrentCity(request.getCurrentCity());
        if (request.getCurrentDistrict() != null) profile.setCurrentDistrict(request.getCurrentDistrict());
        if (request.getCurrentState() != null) profile.setCurrentState(request.getCurrentState());
        if (request.getCurrentPincode() != null) profile.setCurrentPincode(request.getCurrentPincode());
        if (request.getCurrentCountry() != null) profile.setCurrentCountry(request.getCurrentCountry());
        if (request.getPermanentSameAsCurrent() != null) profile.setPermanentSameAsCurrent(request.getPermanentSameAsCurrent());
        if (request.getPermanentAddressLine1() != null) profile.setPermanentAddressLine1(request.getPermanentAddressLine1());
        if (request.getPermanentAddressLine2() != null) profile.setPermanentAddressLine2(request.getPermanentAddressLine2());
        if (request.getPermanentCity() != null) profile.setPermanentCity(request.getPermanentCity());
        if (request.getPermanentDistrict() != null) profile.setPermanentDistrict(request.getPermanentDistrict());
        if (request.getPermanentState() != null) profile.setPermanentState(request.getPermanentState());
        if (request.getPermanentPincode() != null) profile.setPermanentPincode(request.getPermanentPincode());
        if (request.getPermanentCountry() != null) profile.setPermanentCountry(request.getPermanentCountry());

        if (profile.isPermanentSameAsCurrent()) {
            profile.setPermanentAddressLine1(profile.getCurrentAddressLine1());
            profile.setPermanentAddressLine2(profile.getCurrentAddressLine2());
            profile.setPermanentCity(profile.getCurrentCity());
            profile.setPermanentDistrict(profile.getCurrentDistrict());
            profile.setPermanentState(profile.getCurrentState());
            profile.setPermanentPincode(profile.getCurrentPincode());
            profile.setPermanentCountry(profile.getCurrentCountry());
        }

        if (profile.getAddress() == null || profile.getAddress().isBlank()) {
            profile.setAddress(String.join(", ",
                    safe(profile.getCurrentAddressLine1()),
                    safe(profile.getCurrentAddressLine2()),
                    safe(profile.getCurrentCity()),
                    safe(profile.getCurrentState()),
                    safe(profile.getCurrentPincode()),
                    safe(profile.getCurrentCountry())).replaceAll("(, )+", ", ").replaceAll("^, |, $", ""));
        }
    }

    private void validateStep(KycOnboardingStep step, IdentityProfile profile) {
        switch (step) {
            case STEP_1_PERSONAL -> {
                require(profile.getFullName(), "Full name is required");
                if (profile.getDob() == null) throw new RuntimeException("Date of birth is required");
                require(profile.getGender(), "Gender is required");
                require(profile.getNationality(), "Nationality is required");
            }
            case STEP_2_CONTACT -> {
                require(profile.getPhone(), "Phone number is required");
            }
            case STEP_3_ADDRESS -> {
                require(profile.getCurrentAddressLine1(), "Current address line 1 is required");
                require(profile.getCurrentCity(), "Current city is required");
                require(profile.getCurrentState(), "Current state is required");
                require(profile.getCurrentPincode(), "Current pincode is required");
                require(profile.getCurrentCountry(), "Current country is required");
            }
            case STEP_4_PROFILE_PHOTO -> require(profile.getProfilePhotoUrl(), "Profile photo is required");
            case STEP_5_DOCUMENTS, STEP_6_REVIEW_SUBMIT -> {
                // Document checks are enforced in verification submission flow.
            }
        }
    }

    private void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int progressFor(KycOnboardingStep step) {
        return switch (step) {
            case STEP_1_PERSONAL -> 16;
            case STEP_2_CONTACT -> 32;
            case STEP_3_ADDRESS -> 50;
            case STEP_4_PROFILE_PHOTO -> 66;
            case STEP_5_DOCUMENTS -> 83;
            case STEP_6_REVIEW_SUBMIT -> 100;
        };
    }
}
