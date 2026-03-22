package com.trustid.identity.controller;

import java.io.IOException;
import java.nio.file.Files;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trustid.common.dto.ApiResponse;
import com.trustid.identity.dto.PublicQrVerificationResponse;
import com.trustid.identity.service.DigitalIdService;

import lombok.RequiredArgsConstructor;

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

    @GetMapping("/{token}/photo")
    public ResponseEntity<Resource> photoByToken(@PathVariable String token) {
        try {
            Resource resource = digitalIdService.loadPublicTokenProfilePhoto(token);
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            try {
                String probed = Files.probeContentType(resource.getFile().toPath());
                if (probed != null) {
                    mediaType = MediaType.parseMediaType(probed);
                }
            } catch (IOException ignored) {
                // Fall back to octet-stream when content type detection is unavailable.
            }

            return ResponseEntity.ok()
                    .header("Content-Type", mediaType.toString())
                    .body(resource);
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
