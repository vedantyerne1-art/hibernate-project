package com.trustid.verification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {
    List<VerificationRequest> findByUserId(Long userId);
    Optional<VerificationRequest> findFirstByUserIdOrderBySubmittedAtDesc(Long userId);
    Page<VerificationRequest> findByStatus(VerificationRequest.VerificationStatus status, Pageable pageable);
    long countByUserIdAndStatus(Long userId, VerificationRequest.VerificationStatus status);
}
