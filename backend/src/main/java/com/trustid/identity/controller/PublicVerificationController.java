package com.trustid.identity.controller;

import com.trustid.common.dto.ApiResponse;
import com.trustid.identity.dto.PublicQrVerificationResponse;
import com.trustid.identity.service.DigitalIdService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/verify")
@RequiredArgsConstructor
public class PublicVerificationController {

    private final DigitalIdService digitalIdService;

    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<PublicQrVerificationResponse>> verifyByToken(@PathVariable String token) {
        try {
            PublicQrVerificationResponse response = digitalIdService.verifyPublicToken(token);
            return ResponseEntity.ok(ApiResponse.success(response, "Verification details fetched"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired verification token"));
        }
    }
}
