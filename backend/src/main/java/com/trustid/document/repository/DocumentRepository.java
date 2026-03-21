package com.trustid.document.repository;

import com.trustid.document.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<KycDocument, Long> {
    List<KycDocument> findByUserIdAndIsArchivedFalse(Long userId);
    List<KycDocument> findByUserId(Long userId);
    List<KycDocument> findByUserIdAndIsArchived(Long userId, boolean isArchived);
    List<KycDocument> findByUserIdAndDocumentTypeAndIsArchivedFalse(Long userId, com.trustid.common.enums.DocumentType documentType);
    List<KycDocument> findByUserIdAndDocumentNameContainingIgnoreCaseAndIsArchivedFalse(Long userId, String query);
    Optional<KycDocument> findByIdAndUserId(Long id, Long userId);
}
