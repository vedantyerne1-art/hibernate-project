package com.trustid.auth.controller;

import com.trustid.auth.dto.SessionInfoResponse;
import com.trustid.auth.service.SessionService;
import com.trustid.common.dto.ApiResponse;
import com.trustid.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionInfoResponse>>> list(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.listActiveSessions(userDetails.getId()), "Sessions fetched"));
    }

    @PostMapping("/logout-others")
    public ResponseEntity<ApiResponse<Void>> logoutOthers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
        sessionService.revokeOtherSessions(userDetails.getId(), token);
        return ResponseEntity.ok(ApiResponse.success(null, "Other sessions revoked"));
    }
}
