package com.trustid.verification;

import com.trustid.common.enums.DocumentStatus;
import com.trustid.common.enums.DocumentType;
import com.trustid.common.enums.IdentityStatus;
import com.trustid.common.enums.Role;
import com.trustid.document.entity.KycDocument;
import com.trustid.document.repository.DocumentRepository;
import com.trustid.identity.entity.IdentityProfile;
import com.trustid.identity.repository.IdentityRepository;
import com.trustid.user.entity.User;
import com.trustid.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class VerificationServiceTest {

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VerificationService verificationService;

    @Test
    void submitRequestUpdatesProfileAndDocuments() {
        User user = user(10L, "user@trustid.local", Role.USER);
        IdentityProfile profile = profile(20L, 10L, IdentityStatus.DRAFT);
        KycDocument document = document(30L, 10L, DocumentStatus.UPLOADED);

        SubmitVerificationRequest payload = new SubmitVerificationRequest();
        payload.setDocumentIds(List.of(30L));

        when(userRepository.findByEmail("user@trustid.local")).thenReturn(Optional.of(user));
        when(identityRepository.findByUserId(10L)).thenReturn(Optional.of(profile));
        when(documentRepository.findAllById(List.of(30L))).thenReturn(List.of(document));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenAnswer(invocation -> {
            VerificationRequest req = invocation.getArgument(0);
            req.setId(100L);
            return req;
        });

        VerificationRequest saved = verificationService.submitRequest("user@trustid.local", payload);

        assertEquals(100L, saved.getId());
        assertEquals(VerificationRequest.VerificationStatus.PENDING, saved.getStatus());
        assertEquals(IdentityStatus.UNDER_REVIEW, profile.getStatus());
        assertEquals(DocumentStatus.PENDING_REVIEW, document.getStatus());
        assertEquals(1, saved.getDocuments().size());

        verify(documentRepository).saveAll(List.of(document));
        verify(identityRepository).save(profile);
        verify(verificationRequestRepository).save(any(VerificationRequest.class));
    }

    @Test
    void reviewRequestDtoApprovedMarksVerified() {
        User admin = user(1L, "admin@trustid.local", Role.ADMIN);
        IdentityProfile profile = profile(21L, 11L, IdentityStatus.UNDER_REVIEW);
        KycDocument document = document(31L, 11L, DocumentStatus.PENDING_REVIEW);

        VerificationRequest request = new VerificationRequest();
        request.setId(200L);
        request.setUser(user(11L, "user2@trustid.local", Role.USER));
        request.setIdentityProfile(profile);
        request.setStatus(VerificationRequest.VerificationStatus.PENDING);
        request.getDocuments().add(document);

        ReviewVerificationRequest review = new ReviewVerificationRequest();
        review.setStatus(VerificationRequest.VerificationStatus.APPROVED);
        review.setNotes("Looks good");

        when(userRepository.findByEmail("admin@trustid.local")).thenReturn(Optional.of(admin));
        when(verificationRequestRepository.findById(200L)).thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationRequestDTO dto = verificationService.reviewRequestDto("admin@trustid.local", 200L, review);

        assertEquals("APPROVED", dto.getStatus());
        assertEquals("VERIFIED", dto.getDocuments().get(0).getStatus());
        assertEquals("APPROVED", dto.getIdentityProfile().getStatus());
        assertNotNull(dto.getReviewedAt());

        verify(documentRepository).saveAll(request.getDocuments());
        verify(identityRepository).save(profile);
    }

    @Test
    void reviewRequestRejectedUsesNotesWhenReasonBlank() {
        User admin = user(1L, "admin@trustid.local", Role.ADMIN);
        IdentityProfile profile = profile(22L, 12L, IdentityStatus.UNDER_REVIEW);
        KycDocument document = document(32L, 12L, DocumentStatus.PENDING_REVIEW);

        VerificationRequest request = new VerificationRequest();
        request.setId(201L);
        request.setUser(user(12L, "user3@trustid.local", Role.USER));
        request.setIdentityProfile(profile);
        request.setStatus(VerificationRequest.VerificationStatus.PENDING);
        request.getDocuments().add(document);

        ReviewVerificationRequest review = new ReviewVerificationRequest();
        review.setStatus(VerificationRequest.VerificationStatus.REJECTED);
        review.setRejectionReason("   ");
        review.setNotes("Missing details");

        when(userRepository.findByEmail("admin@trustid.local")).thenReturn(Optional.of(admin));
        when(verificationRequestRepository.findById(201L)).thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        verificationService.reviewRequest("admin@trustid.local", 201L, review);

        assertEquals(IdentityStatus.REJECTED, profile.getStatus());
        assertEquals("Missing details", profile.getRejectionReason());
        assertEquals(DocumentStatus.REJECTED, document.getStatus());

        ArgumentCaptor<VerificationRequest> requestCaptor = ArgumentCaptor.forClass(VerificationRequest.class);
        verify(verificationRequestRepository).save(requestCaptor.capture());
        assertEquals(VerificationRequest.VerificationStatus.REJECTED, requestCaptor.getValue().getStatus());
        assertNotNull(requestCaptor.getValue().getReviewedAt());
        verify(identityRepository).save(profile);
    }

    private User user(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .fullName("Test User")
                .email(email)
                .passwordHash("hash")
                .role(role)
                .emailVerified(true)
                .build();
    }

    private IdentityProfile profile(Long id, Long userId, IdentityStatus status) {
        return IdentityProfile.builder()
                .id(id)
                .userId(userId)
                .fullName("Test User")
                .dob(LocalDate.of(2000, 1, 1))
                .status(status)
                .build();
    }

    private KycDocument document(Long id, Long userId, DocumentStatus status) {
        return KycDocument.builder()
                .id(id)
                .userId(userId)
                .documentType(DocumentType.AADHAAR)
                .documentName("Aadhaar")
                .fileName("aadhaar.pdf")
                .filePath("uploads/aadhaar.pdf")
                .mimeType("application/pdf")
                .fileSize(1024L)
                .status(status)
                .build();
    }
}
