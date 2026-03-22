package com.trustid.audit.service;

import com.trustid.audit.entity.AuditLog;
import com.trustid.audit.repository.AuditLogRepository;
import com.trustid.common.enums.AuditAction;
import com.trustid.common.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(Long actorId, Role actorRole, AuditAction action, String entityType, Long entityId, String description, String ipAddress, String userAgent) {
        AuditLog log = AuditLog.builder()
                .actorId(actorId)
                .actorRole(actorRole)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        auditLogRepository.save(log);
    }
}
