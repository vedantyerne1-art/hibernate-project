package com.trustid.developer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trustid.user.entity.User;
import com.trustid.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConsentService {
    private final UserConsentRepository consentRepository;
    private final UserRepository userRepository;

    public java.util.List<UserConsentDTO> getUserConsents(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        java.util.List<UserConsentDTO> result = new java.util.ArrayList<>();
        for (UserConsent consent : consentRepository.findByUserIdAndIsRevokedFalse(user.getId())) {
            result.add(mapToDTO(consent));
        }
        return result;
    }

    @Transactional
    public void revokeConsent(String email, Long consentId) {
        java.util.Objects.requireNonNull(consentId, "consentId is required");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserConsent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new RuntimeException("Consent not found"));

        if (!consent.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to revoke this consent");
        }

        consent.setRevoked(true);
        consent.setUpdatedAt(java.time.LocalDateTime.now());
        consentRepository.save(consent);
    }

    private UserConsentDTO mapToDTO(UserConsent consent) {
        UserConsentDTO dto = new UserConsentDTO();
        dto.setId(consent.getId());
        dto.setClientName(consent.getApiClient().getClientName());
        dto.setOrganizationName(consent.getApiClient().getOrganizationName());
        dto.setScopes(consent.getScopes());
        dto.setGrantedAt(consent.getGrantedAt());
        dto.setExpiresAt(consent.getExpiresAt());
        dto.setRevoked(consent.isRevoked());
        return dto;
    }
}
