package com.trustid.developer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {
    Optional<UserConsent> findByUserIdAndApiClientClientKeyAndIsRevokedFalse(Long userId, String clientKey);
    List<UserConsent> findByUserIdAndIsRevokedFalse(Long userId);
}
