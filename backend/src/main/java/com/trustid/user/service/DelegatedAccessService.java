package com.trustid.user.service;

import com.trustid.user.dto.DelegatedAccessRequest;
import com.trustid.user.entity.DelegatedAccess;
import com.trustid.user.entity.User;
import com.trustid.user.repository.DelegatedAccessRepository;
import com.trustid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DelegatedAccessService {

    private final DelegatedAccessRepository delegatedAccessRepository;
    private final UserRepository userRepository;

    @Transactional
    public DelegatedAccess grant(String ownerEmail, DelegatedAccessRequest request) {
        User owner = userRepository.findByEmail(ownerEmail).orElseThrow(() -> new RuntimeException("Owner not found"));
        if (request.getDelegateUserId() == null || request.getPermission() == null) {
            throw new RuntimeException("Delegate user and permission are required");
        }

        if (owner.getId().equals(request.getDelegateUserId())) {
            throw new RuntimeException("Cannot delegate to self");
        }

        User delegate = userRepository.findById(request.getDelegateUserId())
                .orElseThrow(() -> new RuntimeException("Delegate user not found"));

        DelegatedAccess access = DelegatedAccess.builder()
                .ownerUserId(owner.getId())
                .delegateUserId(delegate.getId())
                .permission(request.getPermission())
                .active(true)
                .expiresAt(request.getExpiresAt())
                .build();

        return delegatedAccessRepository.save(access);
    }

    @Transactional(readOnly = true)
    public List<DelegatedAccess> myDelegations(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail).orElseThrow(() -> new RuntimeException("Owner not found"));
        return delegatedAccessRepository.findByOwnerUserIdAndActiveTrue(owner.getId());
    }

    @Transactional
    public void revoke(String ownerEmail, Long delegationId) {
        User owner = userRepository.findByEmail(ownerEmail).orElseThrow(() -> new RuntimeException("Owner not found"));
        DelegatedAccess access = delegatedAccessRepository.findById(delegationId)
                .orElseThrow(() -> new RuntimeException("Delegated access not found"));
        if (!access.getOwnerUserId().equals(owner.getId())) {
            throw new RuntimeException("Unauthorized revoke operation");
        }
        access.setActive(false);
        delegatedAccessRepository.save(access);
    }
}
