package com.trustid.verification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trustid.audit.service.AuditService;
import com.trustid.common.enums.AuditAction;
import com.trustid.common.enums.DocumentStatus;
import com.trustid.common.enums.IdentityLevel;
import com.trustid.common.enums.IdentityStatus;
import com.trustid.common.enums.NotificationType;
import com.trustid.document.entity.KycDocument;
import com.trustid.document.repository.DocumentRepository;
import com.trustid.identity.entity.IdentityProfile;
import com.trustid.identity.repository.IdentityRepository;
import com.trustid.notification.service.NotificationService;
import com.trustid.user.entity.User;
import com.trustid.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class VerificationService {

    private final VerificationRequestRepository verificationRequestRepository;
    private final IdentityRepository identityProfileRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public VerificationRequest submitRequest(String email, SubmitVerificationRequest requestPayload) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        IdentityProfile profile = identityProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        if (profile.getStatus() == IdentityStatus.APPROVED) {
            throw new RuntimeException("Profile is already verified and APPROVED");
        }

        List<KycDocument> documents = documentRepository.findAllById(requestPayload.getDocumentIds());
        
        // Ensure documents belong to user
        documents.forEach(doc -> {
            if (!doc.getUserId().equals(user.getId())) {
                throw new RuntimeException("Document does not belong to user");
            }
            doc.setStatus(DocumentStatus.PENDING_REVIEW);
        });
        documentRepository.saveAll(documents);

        profile.setStatus(IdentityStatus.UNDER_REVIEW);
        profile.setIdentityLevel(IdentityLevel.LEVEL_3_KYC_SUBMITTED);
        identityProfileRepository.save(profile);

        VerificationRequest req = new VerificationRequest();
        req.setUser(user);
        req.setIdentityProfile(profile);
        req.getDocuments().addAll(documents);
        req.setSubmittedAt(LocalDateTime.now());
        req.setStatus(VerificationRequest.VerificationStatus.PENDING);
        req.setResubmissionReason(null);

        VerificationRequest saved = verificationRequestRepository.save(req);
        auditService.log(user.getId(), user.getRole(), AuditAction.SUBMIT_VERIFICATION, "VERIFICATION_REQUEST", saved.getId(), "Verification request submitted", null, null);
        return saved;
    }

    @Transactional
    public VerificationRequest resubmitRequest(String email, ResubmissionRequest requestPayload) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        IdentityProfile profile = identityProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        if (profile.getStatus() != IdentityStatus.RESUBMISSION_REQUIRED && profile.getStatus() != IdentityStatus.REJECTED) {
            throw new RuntimeException("Resubmission is only allowed when status is RESUBMISSION_REQUIRED or REJECTED");
        }

        List<KycDocument> documents = documentRepository.findAllById(requestPayload.getDocumentIds());
        documents.forEach(doc -> {
            if (!doc.getUserId().equals(user.getId())) {
                throw new RuntimeException("Document does not belong to user");
            }
            doc.setStatus(DocumentStatus.PENDING_REVIEW);
        });
        documentRepository.saveAll(documents);

        profile.setStatus(IdentityStatus.UNDER_REVIEW);
        profile.setRejectionReason(null);
        profile.setIdentityLevel(IdentityLevel.LEVEL_3_KYC_SUBMITTED);
        identityProfileRepository.save(profile);

        VerificationRequest req = new VerificationRequest();
        req.setUser(user);
        req.setIdentityProfile(profile);
        req.getDocuments().addAll(documents);
        req.setSubmittedAt(LocalDateTime.now());
        req.setStatus(VerificationRequest.VerificationStatus.PENDING);
        req.setResubmissionReason(null);

        VerificationRequest saved = verificationRequestRepository.save(req);
        auditService.log(user.getId(), user.getRole(), AuditAction.REQUEST_RESUBMISSION, "VERIFICATION_REQUEST", saved.getId(), "Verification request resubmitted", null, null);
        return saved;
    }

    @Transactional
    public VerificationRequest reviewRequest(String adminEmail, Long requestId, ReviewVerificationRequest reviewData) {
        User admin = userRepository.findByEmail(adminEmail).orElseThrow(() -> new RuntimeException("Admin not found"));
        VerificationRequest req = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        verifyTransition(req.getStatus(), reviewData.getStatus());
        
        req.setAdmin(admin);
        req.setStatus(reviewData.getStatus());
        req.setNotes(reviewData.getNotes());
        req.setRejectionReason(reviewData.getRejectionReason());
        req.setResubmissionReason(reviewData.getResubmissionReason());
        req.setReviewedAt(LocalDateTime.now());

        IdentityProfile profile = req.getIdentityProfile();
        
        switch (reviewData.getStatus()) {
            case APPROVED -> {
                profile.setStatus(IdentityStatus.APPROVED);
                profile.setIdentityLevel(IdentityLevel.LEVEL_4_VERIFIED);
                profile.setApprovedAt(LocalDateTime.now());
                req.getDocuments().forEach(doc -> doc.setStatus(DocumentStatus.VERIFIED));
                documentRepository.saveAll(req.getDocuments());
                notificationService.notifyUser(profile.getUserId(), NotificationType.KYC_APPROVED, "KYC approved", "Your KYC has been approved.", null);
            }
            case REJECTED -> {
                profile.setStatus(IdentityStatus.REJECTED);
                profile.setIdentityLevel(IdentityLevel.LEVEL_2_PROFILE);
                profile.setRejectedAt(LocalDateTime.now());
                profile.setRejectionReason(
                        reviewData.getRejectionReason() != null && !reviewData.getRejectionReason().isBlank()
                                ? reviewData.getRejectionReason()
                                : reviewData.getNotes());
                req.getDocuments().forEach(doc -> doc.setStatus(DocumentStatus.REJECTED));
                documentRepository.saveAll(req.getDocuments());
                notificationService.notifyUser(profile.getUserId(), NotificationType.KYC_REJECTED, "KYC rejected", profile.getRejectionReason(), null);
            }
            case RESUBMISSION_REQUIRED, CHANGES_REQUESTED -> {
                profile.setStatus(IdentityStatus.RESUBMISSION_REQUIRED);
                profile.setIdentityLevel(IdentityLevel.LEVEL_2_PROFILE);
                profile.setRejectedAt(LocalDateTime.now());
                String reason = reviewData.getResubmissionReason() != null && !reviewData.getResubmissionReason().isBlank()
                        ? reviewData.getResubmissionReason()
                        : reviewData.getNotes();
                profile.setRejectionReason(reason);
                req.setResubmissionReason(reason);
                req.getDocuments().forEach(doc -> {
                    if (doc.getStatus() != DocumentStatus.VERIFIED) {
                        doc.setStatus(DocumentStatus.REJECTED);
                    }
                });
                documentRepository.saveAll(req.getDocuments());
                notificationService.notifyUser(profile.getUserId(), NotificationType.RESUBMISSION_REQUIRED, "Resubmission required", reason, null);
            }
            case IN_REVIEW -> profile.setStatus(IdentityStatus.UNDER_REVIEW);
            default -> {
                // Keep status unchanged for any unsupported transition.
            }
        }

        identityProfileRepository.save(profile);
        VerificationRequest saved = verificationRequestRepository.save(req);
        switch (reviewData.getStatus()) {
            case APPROVED -> auditService.log(admin.getId(), admin.getRole(), AuditAction.APPROVE_VERIFICATION, "VERIFICATION_REQUEST", saved.getId(), "Admin approved KYC", null, null);
            case REJECTED -> auditService.log(admin.getId(), admin.getRole(), AuditAction.REJECT_VERIFICATION, "VERIFICATION_REQUEST", saved.getId(), "Admin rejected KYC", null, null);
            case RESUBMISSION_REQUIRED, CHANGES_REQUESTED ->
                    auditService.log(admin.getId(), admin.getRole(), AuditAction.REQUEST_RESUBMISSION, "VERIFICATION_REQUEST", saved.getId(), "Admin requested resubmission", null, null);
            default -> {
                // No audit event for passive status updates.
            }
        }
        return saved;
    }

    @Transactional
    public VerificationRequestDTO reviewRequestDto(String adminEmail, Long requestId, ReviewVerificationRequest reviewData) {
        return toDto(reviewRequest(adminEmail, requestId, reviewData));
    }

    public List<VerificationRequest> getRequestsByStatus(VerificationRequest.VerificationStatus status) {
        return verificationRequestRepository.findByStatus(status, org.springframework.data.domain.Pageable.unpaged()).getContent();
    }

    @Transactional(readOnly = true)
    public List<VerificationRequestDTO> getRequestsByStatusDto(VerificationRequest.VerificationStatus status) {
        return verificationRequestRepository.findByStatus(status, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private VerificationRequestDTO toDto(VerificationRequest request) {
        return VerificationRequestDTO.builder()
                .id(request.getId())
                .status(request.getStatus().name())
                .notes(request.getNotes())
                .rejectionReason(request.getRejectionReason())
                .resubmissionReason(request.getResubmissionReason())
                .submittedAt(request.getSubmittedAt())
                .reviewedAt(request.getReviewedAt())
                .user(VerificationRequestDTO.UserSummary.builder()
                        .id(request.getUser().getId())
                        .fullName(request.getUser().getFullName())
                        .email(request.getUser().getEmail())
                        .build())
                .identityProfile(VerificationRequestDTO.IdentitySummary.builder()
                        .id(request.getIdentityProfile().getId())
                        .fullName(request.getIdentityProfile().getFullName())
                        .identityNumber(request.getIdentityProfile().getIdentityNumber())
                        .dob(request.getIdentityProfile().getDob())
                        .status(request.getIdentityProfile().getStatus().name())
                        .build())
                .documents(request.getDocuments().stream().map(doc -> VerificationRequestDTO.DocumentSummary.builder()
                        .id(doc.getId())
                        .documentName(doc.getDocumentName())
                        .documentType(doc.getDocumentType().name())
                        .documentNumber(doc.getDocumentNumber())
                        .status(doc.getStatus().name())
                        .fileName(doc.getFileName())
                        .mimeType(doc.getMimeType())
                        .fileSize(doc.getFileSize())
                    .versionNumber(doc.getVersionNumber())
                    .ocrName(doc.getOcrName())
                    .ocrDob(doc.getOcrDob())
                    .ocrDocumentNumber(doc.getOcrDocumentNumber())
                    .comparisonWarning(doc.getComparisonWarning())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    private void verifyTransition(VerificationRequest.VerificationStatus from, VerificationRequest.VerificationStatus to) {
        if (to == null) {
            throw new RuntimeException("Target status is required");
        }
        if (from == VerificationRequest.VerificationStatus.APPROVED || from == VerificationRequest.VerificationStatus.REJECTED) {
            throw new RuntimeException("Finalized requests cannot be changed");
        }
        if (from == VerificationRequest.VerificationStatus.PENDING && to == VerificationRequest.VerificationStatus.PENDING) {
            throw new RuntimeException("No-op transition is not allowed");
        }
    }
}
