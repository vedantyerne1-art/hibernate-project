package com.trustid.identity.repository;

import com.trustid.identity.entity.IdentityProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdentityRepository extends JpaRepository<IdentityProfile, Long> {
    Optional<IdentityProfile> findByUserId(Long userId);
    Optional<IdentityProfile> findByIdentityNumber(String identityNumber);
}
