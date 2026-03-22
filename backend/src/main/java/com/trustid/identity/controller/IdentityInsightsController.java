package com.trustid.identity.controller;

import com.trustid.common.dto.ApiResponse;
import com.trustid.common.enums.RiskLevel;
import com.trustid.document.entity.DocumentAccessLog;
import com.trustid.identity.dto.AdminRiskUserResponse;
import com.trustid.identity.dto.IdentityInsightsResponse;
import com.trustid.identity.dto.TimelineEventResponse;
import com.trustid.identity.service.IdentityInsightsService;
import com.trustid.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/identity/insights")
@RequiredArgsConstructor
public class IdentityInsightsController {

    private final IdentityInsightsService identityInsightsService;

    @GetMapping
    public ResponseEntity<ApiResponse<IdentityInsightsResponse>> evaluate(@AuthenticationPrincipal CustomUserDetails userDetails) {
        IdentityInsightsResponse response = identityInsightsService.evaluate(userDetails.getId(), userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Identity insights evaluated"));
    }

    @GetMapping("/timeline")
    public ResponseEntity<ApiResponse<List<TimelineEventResponse>>> timeline(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(identityInsightsService.timeline(userDetails.getId()), "Timeline fetched"));
    }

    @GetMapping("/access-logs")
    public ResponseEntity<ApiResponse<List<DocumentAccessLog>>> accessLogs(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(identityInsightsService.accessLogs(userDetails.getId()), "Access logs fetched"));
    }

    @GetMapping("/admin/risk-users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminRiskUserResponse>>> adminRiskUsers(
            @RequestParam(defaultValue = "HIGH") RiskLevel riskLevel) {
        return ResponseEntity.ok(ApiResponse.success(identityInsightsService.adminRiskUsers(riskLevel), "Admin risk users fetched"));
    }
}
