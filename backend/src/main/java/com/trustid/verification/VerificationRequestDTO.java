package com.trustid.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerificationRequestDTO {
    private Long id;
    private String status;
    private String notes;
    private String rejectionReason;
    private String resubmissionReason;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private UserSummary user;
    private IdentitySummary identityProfile;
    private List<DocumentSummary> documents;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserSummary {
        private Long id;
        private String fullName;
        private String email;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IdentitySummary {
        private Long id;
        private String fullName;
        private String identityNumber;
        private LocalDate dob;
        private String status;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DocumentSummary {
        private Long id;
        private String documentName;
        private String documentType;
        private String documentNumber;
        private String status;
        private String fileName;
        private String mimeType;
        private Long fileSize;
    }
}
