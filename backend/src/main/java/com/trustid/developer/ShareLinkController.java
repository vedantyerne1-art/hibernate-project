package com.trustid.developer;

import com.trustid.common.dto.ApiResponse;
import com.trustid.developer.dto.ShareLinkRequest;
import com.trustid.document.entity.KycDocument;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    @PostMapping("/links")
    public ResponseEntity<ApiResponse<Map<String, String>>> create(Authentication authentication, @RequestBody ShareLinkRequest request) {
        String url = shareLinkService.createShareLink(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url), "Share link created"));
    }

    @GetMapping("/links/{token}")
    public ResponseEntity<ApiResponse<List<KycDocument>>> resolve(@PathVariable String token, HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For") != null ? request.getHeader("X-Forwarded-For") : request.getRemoteAddr();
        List<KycDocument> docs = shareLinkService.resolveShareLink(token, "public-access", ip, request.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success(docs, "Shared documents resolved"));
    }

    @DeleteMapping("/links/{id}")
    public ResponseEntity<ApiResponse<Void>> revoke(Authentication authentication, @PathVariable Long id) {
        shareLinkService.revokeLink(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Share link revoked"));
    }
}
