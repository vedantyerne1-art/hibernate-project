package com.trustid.verification;

import lombok.Data;

@Data
public class ReviewVerificationRequest {
    private VerificationRequest.VerificationStatus status; // APPROVED, REJECTED
    private String rejectionReason;
    private String resubmissionReason;
    private String notes;
}
