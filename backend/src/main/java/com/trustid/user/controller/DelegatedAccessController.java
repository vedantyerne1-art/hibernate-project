package com.trustid.user.controller;

import com.trustid.common.dto.ApiResponse;
import com.trustid.user.dto.DelegatedAccessRequest;
import com.trustid.user.entity.DelegatedAccess;
import com.trustid.user.service.DelegatedAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delegations")
@RequiredArgsConstructor
public class DelegatedAccessController {

    private final DelegatedAccessService delegatedAccessService;

    @PostMapping
    public ResponseEntity<ApiResponse<DelegatedAccess>> grant(Authentication authentication, @RequestBody DelegatedAccessRequest request) {
        return ResponseEntity.ok(ApiResponse.success(delegatedAccessService.grant(authentication.getName(), request), "Delegated access granted"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DelegatedAccess>>> list(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(delegatedAccessService.myDelegations(authentication.getName()), "Delegations fetched"));
    }

    @DeleteMapping("/{delegationId}")
    public ResponseEntity<ApiResponse<Void>> revoke(Authentication authentication, @PathVariable Long delegationId) {
        delegatedAccessService.revoke(authentication.getName(), delegationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Delegation revoked"));
    }
}
