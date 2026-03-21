package com.trustid.document.entity;

import com.trustid.common.base.AuditableEntity;
import com.trustid.common.enums.DocumentCategory;
import com.trustid.common.enums.DocumentStatus;
import com.trustid.common.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycDocument extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private Long identityProfileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private String documentName;

    private String documentNumber;
    private String holderName;
    private LocalDate issueDate;
    private LocalDate expiryDate;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    private String backFileName;
    private String backFilePath;

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DocumentCategory documentCategory = DocumentCategory.KYC;

    @Column(nullable = false)
    @Builder.Default
    private boolean isArchived = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isShared = false;

    private LocalDateTime uploadedAt;
    private LocalDateTime reviewedAt;

    @Column(columnDefinition = "TEXT")
    private String reviewRemarks;

    @Version
    private Long version;
}
