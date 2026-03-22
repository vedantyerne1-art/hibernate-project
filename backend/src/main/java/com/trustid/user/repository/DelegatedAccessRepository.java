package com.trustid.user.repository;

import com.trustid.user.entity.DelegatedAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DelegatedAccessRepository extends JpaRepository<DelegatedAccess, Long> {
    List<DelegatedAccess> findByOwnerUserIdAndActiveTrue(Long ownerUserId);
    List<DelegatedAccess> findByDelegateUserIdAndActiveTrue(Long delegateUserId);
}
