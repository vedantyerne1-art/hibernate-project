package com.trustid.developer;

import com.trustid.identity.entity.IdentityProfile;
import com.trustid.identity.repository.IdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/developer")
@RequiredArgsConstructor
public class DeveloperApiController {

    private final ApiClientRepository apiClientRepository;
    private final UserConsentRepository consentRepository;
    private final IdentityRepository identityProfileRepository;

    @GetMapping("/verify-identity/{trustIdNumber}")
    public ResponseEntity<?> verifyIdentity(
            @RequestHeader("X-API-KEY") String apiKey,
            @PathVariable String trustIdNumber) {
        
        ApiClient client = apiClientRepository.findByClientKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Invalid API Key"));

        if (!client.isActive()) {
            return ResponseEntity.status(403).body("API Client is inactive");
        }

        IdentityProfile profile = identityProfileRepository.findByIdentityNumber(trustIdNumber)
                .orElseThrow(() -> new RuntimeException("Identity not found"));

        UserConsent consent = consentRepository.findByUserIdAndApiClientClientKeyAndIsRevokedFalse(profile.getUserId(), apiKey)
                .orElseThrow(() -> new RuntimeException("User has not granted consent to this API Client"));

        if (consent.getScopes().contains("PROFILE")) {
            return ResponseEntity.ok(profile);
        } else {
            return ResponseEntity.status(403).body("Scope PROFILE not granted");
        }
    }
}
