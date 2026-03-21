package com.trustid.identity.controller;

import com.trustid.common.dto.ApiResponse;
import com.trustid.identity.dto.IdentityCreateRequest;
import com.trustid.identity.dto.IdentityResponse;
import com.trustid.identity.dto.OnboardingStateResponse;
import com.trustid.identity.dto.OnboardingStepRequest;
import com.trustid.identity.dto.QrTokenResponse;
import com.trustid.identity.service.IdentityService;
import com.trustid.identity.service.QrVerificationService;
import com.trustid.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityService identityService;
    private final QrVerificationService qrVerificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<IdentityResponse>> createOrUpdate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody IdentityCreateRequest request) {
        IdentityResponse response = identityService.createOrUpdateIdentity(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Identity saved successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<IdentityResponse>> getMyIdentity(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        IdentityResponse response = identityService.getIdentity(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Identity retrieved successfully"));
    }

    @PutMapping("/onboarding/step")
    public ResponseEntity<ApiResponse<OnboardingStateResponse>> saveStep(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OnboardingStepRequest request) {
        OnboardingStateResponse response = identityService.saveOnboardingStep(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Onboarding step saved"));
    }

    @GetMapping("/onboarding")
    public ResponseEntity<ApiResponse<OnboardingStateResponse>> getOnboarding(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        OnboardingStateResponse response = identityService.getOnboardingState(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Onboarding state fetched"));
    }

    @PostMapping("/onboarding/submit")
    public ResponseEntity<ApiResponse<OnboardingStateResponse>> submitOnboarding(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        OnboardingStateResponse response = identityService.submitOnboarding(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "KYC submitted for review"));
    }

    @PostMapping("/onboarding/profile-photo")
    public ResponseEntity<ApiResponse<IdentityResponse>> uploadProfilePhoto(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        IdentityResponse response = identityService.uploadProfilePhoto(userDetails.getId(), file);
        return ResponseEntity.ok(ApiResponse.success(response, "Profile photo uploaded"));
    }

    @GetMapping("/me/qr-token")
    public ResponseEntity<ApiResponse<QrTokenResponse>> generateMyQrToken(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        QrTokenResponse response = qrVerificationService.generateForUser(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "QR token generated successfully"));
    }
}
