package com.trustid.identity.service;

import com.trustid.audit.entity.AuditLog;
import com.trustid.audit.repository.AuditLogRepository;
import com.trustid.auth.repository.OtpTokenRepository;
import com.trustid.common.enums.IdentityLevel;
import com.trustid.common.enums.IdentityStatus;
import com.trustid.common.enums.RiskLevel;
import com.trustid.document.entity.DocumentAccessLog;
import com.trustid.document.entity.KycDocument;
import com.trustid.document.repository.DocumentAccessLogRepository;
import com.trustid.document.repository.DocumentRepository;
import com.trustid.identity.dto.IdentityInsightsResponse;
import com.trustid.identity.dto.AdminRiskUserResponse;
import com.trustid.identity.dto.TimelineEventResponse;
import com.trustid.identity.entity.IdentityProfile;
import com.trustid.identity.repository.IdentityRepository;
import com.trustid.user.repository.UserRepository;
import com.trustid.verification.VerificationRequest;
import com.trustid.verification.VerificationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class IdentityInsightsService {

    private final IdentityRepository identityRepository;
    private final DocumentRepository documentRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final DocumentAccessLogRepository documentAccessLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public IdentityInsightsResponse evaluate(Long userId, String email) {
        IdentityProfile profile = identityRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Identity profile not found"));

        List<KycDocument> docs = documentRepository.findByUserIdAndIsArchivedFalse(userId);
        int profileScore = profileCompletenessScore(profile);
        int verifiedDocs = (int) docs.stream().filter(d -> d.getStatus() == com.trustid.common.enums.DocumentStatus.VERIFIED).count();
        int validExpiringDocs = (int) docs.stream()
                .filter(d -> d.getExpiryDate() == null || !d.getExpiryDate().isBefore(LocalDate.now()))
                .count();

        int trustScore = Math.min(100, profileScore + (verifiedDocs * 15) + (validExpiringDocs * 5));
        IdentityLevel identityLevel = resolveIdentityLevel(profile);
        RiskLevel riskLevel = resolveRiskLevel(userId, email);

        profile.setTrustScore(trustScore);
        profile.setIdentityLevel(identityLevel);
        profile.setRiskLevel(riskLevel);
        profile.setLastRiskEvaluatedAt(LocalDateTime.now());
        identityRepository.save(profile);

        return IdentityInsightsResponse.builder()
                .trustScore(trustScore)
                .identityLevel(identityLevel)
                .riskLevel(riskLevel)
                .suggestions(suggestions(profile, docs, trustScore))
                .alerts(alerts(profile, docs, riskLevel))
                .build();
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> timeline(Long userId) {
        List<TimelineEventResponse> events = new ArrayList<>();

        for (AuditLog log : auditLogRepository.findTop100ByActorIdOrderByCreatedAtDesc(userId)) {
            events.add(TimelineEventResponse.builder()
                    .eventType(log.getAction().name())
                    .description(log.getDescription())
                    .occurredAt(log.getCreatedAt())
                    .build());
        }

        for (DocumentAccessLog access : documentAccessLogRepository.findTop50ByOwnerUserIdOrderByCreatedAtDesc(userId)) {
            events.add(TimelineEventResponse.builder()
                    .eventType("ACCESS")
                    .description((access.getAccessorEmail() == null ? "Someone" : access.getAccessorEmail()) + " accessed document data")
                    .occurredAt(access.getCreatedAt())
                    .build());
        }

        return events.stream()
                .sorted(Comparator.comparing(TimelineEventResponse::getOccurredAt).reversed())
                .limit(100)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentAccessLog> accessLogs(Long userId) {
        return documentAccessLogRepository.findTop50ByOwnerUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<AdminRiskUserResponse> adminRiskUsers(RiskLevel riskLevel) {
        return identityRepository.findTop100ByRiskLevelOrderByUpdatedAtDesc(riskLevel)
                .stream()
                .map(profile -> {
                    String email = userRepository.findById(profile.getUserId())
                            .map(user -> user.getEmail())
                            .orElse("-");

                    return AdminRiskUserResponse.builder()
                            .userId(profile.getUserId())
                            .fullName(profile.getFullName())
                            .email(email)
                            .trustScore(profile.getTrustScore())
                            .identityLevel(profile.getIdentityLevel())
                            .status(profile.getStatus())
                            .riskLevel(profile.getRiskLevel())
                            .build();
                })
                .toList();
    }

    private int profileCompletenessScore(IdentityProfile profile) {
        int score = 0;
        if (notBlank(profile.getFullName())) score += 12;
        if (profile.getDob() != null) score += 10;
        if (notBlank(profile.getGender())) score += 6;
        if (notBlank(profile.getNationality())) score += 8;
        if (notBlank(profile.getPhone())) score += 8;
        if (notBlank(profile.getCurrentAddressLine1())) score += 8;
        if (notBlank(profile.getCurrentCity())) score += 8;
        if (notBlank(profile.getCurrentState())) score += 8;
        if (notBlank(profile.getCurrentCountry())) score += 8;
        if (notBlank(profile.getProfilePhotoUrl())) score += 12;
        if (profile.isOnboardingCompleted()) score += 20;
        return score;
    }

    private IdentityLevel resolveIdentityLevel(IdentityProfile profile) {
        if (profile.getStatus() == IdentityStatus.APPROVED) return IdentityLevel.LEVEL_4_VERIFIED;
        if (profile.getStatus() == IdentityStatus.UNDER_REVIEW || profile.getStatus() == IdentityStatus.PENDING) return IdentityLevel.LEVEL_3_KYC_SUBMITTED;
        if (profile.getOnboardingProgress() != null && profile.getOnboardingProgress() >= 40) return IdentityLevel.LEVEL_2_PROFILE;
        return IdentityLevel.LEVEL_1_BASIC;
    }

    private RiskLevel resolveRiskLevel(Long userId, String email) {
        long rejectedCount = verificationRequestRepository.countByUserIdAndStatus(userId, VerificationRequest.VerificationStatus.REJECTED);
        int otpAttempts = otpTokenRepository.findTopByEmailAndOtpTypeOrderByCreatedAtDesc(email, com.trustid.common.enums.OtpType.EMAIL_VERIFICATION)
                .map(token -> token.getAttemptCount() + token.getResendCount())
                .orElse(0);

        if (rejectedCount >= 2 || otpAttempts >= 8) {
            return RiskLevel.HIGH;
        }
        if (rejectedCount >= 1 || otpAttempts >= 4) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private List<String> suggestions(IdentityProfile profile, List<KycDocument> docs, int trustScore) {
        List<String> suggestions = new ArrayList<>();
        if (!notBlank(profile.getProfilePhotoUrl())) suggestions.add("Upload a profile photo to complete KYC.");
        if (docs.stream().noneMatch(d -> d.getDocumentType() == com.trustid.common.enums.DocumentType.AADHAAR)) suggestions.add("Add Aadhaar document for faster verification.");
        if (docs.stream().noneMatch(d -> d.getDocumentType() == com.trustid.common.enums.DocumentType.PAN)) suggestions.add("Add PAN document to improve trust score.");
        if (trustScore < 70) suggestions.add("Complete all profile and address fields to increase trust score.");
        if (suggestions.isEmpty()) suggestions.add("Your profile is strong. Keep documents updated.");
        return suggestions;
    }

    private List<String> alerts(IdentityProfile profile, List<KycDocument> docs, RiskLevel riskLevel) {
        List<String> alerts = new ArrayList<>();
        if (riskLevel == RiskLevel.HIGH) alerts.add("High risk detected due to repeated OTP/review failures.");
        docs.stream()
                .filter(d -> d.getExpiryDate() != null && !d.getExpiryDate().isAfter(LocalDate.now().plusDays(30)))
                .forEach(d -> alerts.add("Document nearing expiry: " + d.getDocumentName()));
        if (profile.getStatus() == IdentityStatus.RESUBMISSION_REQUIRED) {
            alerts.add("Resubmission required. Review admin remarks and upload corrected files.");
        }
        return alerts;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
