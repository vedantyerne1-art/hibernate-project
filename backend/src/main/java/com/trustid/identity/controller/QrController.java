package com.trustid.identity.controller;

import com.trustid.common.dto.ApiResponse;
import com.trustid.identity.dto.DigitalIdCardResponse;
import com.trustid.identity.service.DigitalIdService;
import com.trustid.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/qr")
@RequiredArgsConstructor
public class QrController {

    private final DigitalIdService digitalIdService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<DigitalIdCardResponse>> generate(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        DigitalIdCardResponse response = digitalIdService.generateDigitalId(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Digital ID generated"));
    }
}
