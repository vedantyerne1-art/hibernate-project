package com.trustid.identity.controller;

import java.io.IOException;
import java.nio.file.Files;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trustid.common.dto.ApiResponse;
import com.trustid.identity.dto.PublicQrVerificationResponse;
import com.trustid.identity.service.QrVerificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/qr")
@RequiredArgsConstructor
public class PublicQrVerificationController {

    private final QrVerificationService qrVerificationService;

    @GetMapping("/resolve")
    public ResponseEntity<ApiResponse<PublicQrVerificationResponse>> resolve(@RequestParam String token) {
        try {
            PublicQrVerificationResponse response = qrVerificationService.resolve(token);
            return ResponseEntity.ok(ApiResponse.success(response, "QR token resolved successfully"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired QR token"));
        }
    }

    @GetMapping("/photo")
    public ResponseEntity<Resource> photo(@RequestParam String token) {
        try {
            Resource resource = qrVerificationService.loadProfilePhoto(token);
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
