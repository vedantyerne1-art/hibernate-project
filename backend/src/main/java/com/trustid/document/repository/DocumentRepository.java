package com.trustid.document.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trustid.document.entity.KycDocument;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface DocumentRepository extends JpaRepository<KycDocument, Long> {
    List<KycDocument> findByUserIdAndIsArchivedFalse(Long userId);
    List<KycDocument> findByUserId(Long userId);
    List<KycDocument> findByUserIdAndIsArchived(Long userId, boolean isArchived);
    List<KycDocument> findByUserIdAndDocumentTypeAndIsArchivedFalse(Long userId, com.trustid.common.enums.DocumentType documentType);
    List<KycDocument> findByUserIdAndDocumentNameContainingIgnoreCaseAndIsArchivedFalse(Long userId, String query);
    Optional<KycDocument> findByIdAndUserId(Long id, Long userId);
    Optional<KycDocument> findTopByUserIdAndDocumentTypeAndDocumentNumberAndSupersededFalseOrderByVersionNumberDesc(
            Long userId,
            com.trustid.common.enums.DocumentType documentType,
            String documentNumber
    );

        @Query("""
            select d from KycDocument d
            where d.userId = :userId
              and (
               d.id = :documentId
               or d.previousVersionId = :documentId
               or d.id in (select x.previousVersionId from KycDocument x where x.id = :documentId and x.previousVersionId is not null)
              )
            order by d.versionNumber asc
            """)
        List<KycDocument> findVersionHistory(@Param("userId") Long userId, @Param("documentId") Long documentId);

    List<KycDocument> findByUserIdAndExpiryDateIsNotNullAndIsArchivedFalse(Long userId);
    List<KycDocument> findByUserIdAndDocumentCategoryAndIsArchivedFalse(Long userId, com.trustid.common.enums.DocumentCategory category);
}
