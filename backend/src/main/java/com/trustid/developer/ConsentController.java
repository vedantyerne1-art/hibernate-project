package com.trustid.developer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consents")
@RequiredArgsConstructor
public class ConsentController {
    
    private final ConsentService consentService;

    @GetMapping
    public ResponseEntity<?> getMyConsents(Authentication authentication) {
        try {
            return ResponseEntity.ok(consentService.getUserConsents(authentication.getName()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{consentId}")
    public ResponseEntity<?> revokeConsent(Authentication authentication, @PathVariable Long consentId) {
        try {
            consentService.revokeConsent(authentication.getName(), consentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
