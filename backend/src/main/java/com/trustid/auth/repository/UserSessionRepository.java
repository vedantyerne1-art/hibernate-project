package com.trustid.auth.repository;

import com.trustid.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    List<UserSession> findByUserIdAndRevokedFalseOrderByLastActiveAtDesc(Long userId);
    Optional<UserSession> findBySessionToken(String sessionToken);
}
