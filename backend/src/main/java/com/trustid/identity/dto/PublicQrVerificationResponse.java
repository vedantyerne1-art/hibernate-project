package com.trustid.identity.dto;

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
public class PublicQrVerificationResponse {
    private boolean valid;
    private boolean verified;
    private String fullName;
    private String identityNumber;
    private String profilePhotoUrl;
    private String status;
    private LocalDate issueDate;
    private LocalDateTime expiresAt;
}
