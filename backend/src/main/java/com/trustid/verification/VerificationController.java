package com.trustid.verification;

import com.trustid.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping("/submit")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VerificationRequest>> submitVerification(
            Authentication authentication,
            @RequestBody SubmitVerificationRequest request) {
        try {
            VerificationRequest req = verificationService.submitRequest(authentication.getName(), request);
            return ResponseEntity.ok(ApiResponse.success(req, "Verification request submitted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/resubmit")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VerificationRequest>> resubmitVerification(
            Authentication authentication,
            @RequestBody ResubmissionRequest request) {
        try {
            VerificationRequest req = verificationService.resubmitRequest(authentication.getName(), request);
            return ResponseEntity.ok(ApiResponse.success(req, "Verification resubmitted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/admin/review/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VerificationRequestDTO>> reviewVerification(
            Authentication authentication,
            @PathVariable Long requestId,
            @RequestBody ReviewVerificationRequest reviewData) {
        try {
            VerificationRequestDTO req = verificationService.reviewRequestDto(authentication.getName(), requestId, reviewData);
            return ResponseEntity.ok(ApiResponse.success(req, "Verification request reviewed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/admin/requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<VerificationRequestDTO>>> getPendingRequests(
            @RequestParam(defaultValue = "PENDING") VerificationRequest.VerificationStatus status) {
        return ResponseEntity.ok(ApiResponse.success(verificationService.getRequestsByStatusDto(status), "Verification requests fetched"));
    }
}
