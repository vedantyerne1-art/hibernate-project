package com.trustid.document.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trustid.document.entity.DocumentAccessLog;

public interface DocumentAccessLogRepository extends JpaRepository<DocumentAccessLog, Long> {
    List<DocumentAccessLog> findTop50ByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);
}
