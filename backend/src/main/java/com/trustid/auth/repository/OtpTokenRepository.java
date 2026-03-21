package com.trustid.auth.repository;

import com.trustid.auth.entity.OtpToken;
import com.trustid.common.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByEmailAndOtpTypeOrderByCreatedAtDesc(String email, OtpType type);
}
