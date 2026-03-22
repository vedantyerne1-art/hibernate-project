package com.trustid.document.service;

import com.trustid.common.enums.DocumentCategory;
import com.trustid.common.enums.DocumentStatus;
import com.trustid.common.enums.DocumentType;
import com.trustid.common.enums.AuditAction;
import com.trustid.document.dto.LockerDocumentRequest;
import com.trustid.document.dto.OcrExtractionResult;
import com.trustid.document.entity.DocumentAccessLog;
import com.trustid.document.entity.DocumentFolder;
import com.trustid.document.entity.KycDocument;
import com.trustid.document.repository.DocumentAccessLogRepository;
import com.trustid.document.repository.DocumentRepository;
import com.trustid.document.repository.DocumentFolderRepository;
import com.trustid.identity.entity.IdentityProfile;
import com.trustid.identity.repository.IdentityRepository;
import com.trustid.audit.service.AuditService;
import com.trustid.common.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class LockerService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final OcrService ocrService;
    private final IdentityRepository identityRepository;
    private final DocumentFolderRepository folderRepository;
    private final DocumentAccessLogRepository documentAccessLogRepository;
    private final AuditService auditService;

    @Transactional
    public KycDocument upload(Long userId, MultipartFile frontFile, MultipartFile backFile, LockerDocumentRequest request) {
        if (frontFile == null || frontFile.isEmpty()) {
            throw new RuntimeException("Front file is required");
        }

        String frontName = fileStorageService.storeFile(frontFile, userId);
        String backName = (backFile != null && !backFile.isEmpty()) ? fileStorageService.storeFile(backFile, userId) : null;

        KycDocument previousVersion = null;
        if (request.getDocumentNumber() != null && !request.getDocumentNumber().isBlank()) {
            previousVersion = documentRepository
                .findTopByUserIdAndDocumentTypeAndDocumentNumberAndSupersededFalseOrderByVersionNumberDesc(
                    userId,
                    request.getDocumentType() == null ? DocumentType.OTHER : request.getDocumentType(),
                    request.getDocumentNumber())
                .orElse(null);
        }

        OcrExtractionResult ocr = ocrService.extract(frontFile);
        IdentityProfile profile = identityRepository.findByUserId(userId).orElse(null);
        String comparisonWarning = buildComparisonWarning(profile, ocr);

        KycDocument doc = KycDocument.builder()
                .userId(userId)
                .documentType(request.getDocumentType() == null ? DocumentType.OTHER : request.getDocumentType())
                .documentCategory(request.getDocumentCategory() == null ? DocumentCategory.KYC : request.getDocumentCategory())
                .documentName((request.getDocumentLabel() == null || request.getDocumentLabel().isBlank()) ? "Untitled Document" : request.getDocumentLabel())
                .documentNumber(request.getDocumentNumber())
                .holderName(request.getHolderName())
                .issueDate(request.getIssueDate())
                .expiryDate(request.getExpiryDate())
            .folderName(request.getFolderName())
            .tags(request.getTags())
                .fileName(frontName)
                .filePath(frontName)
                .backFileName(backName)
                .backFilePath(backName)
                .mimeType(frontFile.getContentType())
                .fileSize(frontFile.getSize())
                .status(DocumentStatus.UPLOADED)
                .isArchived(false)
                .isShared(false)
            .previousVersionId(previousVersion == null ? null : previousVersion.getId())
            .versionNumber(previousVersion == null ? 1 : (previousVersion.getVersionNumber() == null ? 1 : previousVersion.getVersionNumber() + 1))
            .superseded(false)
            .ocrExtractedText(ocr.getRawText())
            .ocrName(ocr.getExtractedName())
            .ocrDob(ocr.getExtractedDob())
            .ocrDocumentNumber(ocr.getExtractedDocumentNumber())
            .comparisonWarning(comparisonWarning)
                .uploadedAt(LocalDateTime.now())
                .build();

        if (previousVersion != null) {
            previousVersion.setSuperseded(true);
            documentRepository.save(previousVersion);
        }

        KycDocument saved = documentRepository.save(doc);
        auditService.log(userId, Role.USER, AuditAction.UPLOAD_DOCUMENT, "KYC_DOCUMENT", saved.getId(), "Document uploaded to locker", null, null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<KycDocument> list(Long userId, DocumentType type, String q, Boolean archived) {
        boolean includeArchived = archived != null && archived;
        if (type != null) {
            return includeArchived
                    ? documentRepository.findByUserId(userId).stream().filter(d -> d.getDocumentType() == type).toList()
                    : documentRepository.findByUserIdAndDocumentTypeAndIsArchivedFalse(userId, type);
        }
        if (q != null && !q.isBlank()) {
            return includeArchived
                    ? documentRepository.findByUserId(userId).stream().filter(d -> d.getDocumentName() != null && d.getDocumentName().toLowerCase().contains(q.toLowerCase())).toList()
                    : documentRepository.findByUserIdAndDocumentNameContainingIgnoreCaseAndIsArchivedFalse(userId, q);
        }
        return includeArchived ? documentRepository.findByUserIdAndIsArchived(userId, true) : documentRepository.findByUserIdAndIsArchivedFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<KycDocument> listByCategory(Long userId, DocumentCategory category) {
        return documentRepository.findByUserIdAndDocumentCategoryAndIsArchivedFalse(userId, category);
    }

    @Transactional(readOnly = true)
    public List<KycDocument> versionHistory(Long userId, Long documentId) {
        userDocument(userId, documentId);
        return documentRepository.findVersionHistory(userId, documentId);
    }

    @Transactional
    public DocumentFolder createFolder(Long userId, String folderName, Long parentFolderId) {
        if (folderName == null || folderName.isBlank()) {
            throw new RuntimeException("Folder name is required");
        }
        DocumentFolder folder = DocumentFolder.builder()
                .userId(userId)
                .folderName(folderName.trim())
                .parentFolderId(parentFolderId)
                .build();
        return folderRepository.save(folder);
    }

    @Transactional(readOnly = true)
    public List<DocumentFolder> listFolders(Long userId) {
        return folderRepository.findByUserIdOrderByFolderNameAsc(userId);
    }

    @Transactional(readOnly = true)
    public List<KycDocument> expiringDocuments(Long userId, int days) {
        LocalDate threshold = LocalDate.now().plusDays(Math.max(days, 0));
        return documentRepository.findByUserIdAndExpiryDateIsNotNullAndIsArchivedFalse(userId)
                .stream()
                .filter(doc -> doc.getExpiryDate() != null && !doc.getExpiryDate().isAfter(threshold))
                .toList();
    }

    @Transactional(readOnly = true)
    public String compareDocuments(Long userId, Long firstDocumentId, Long secondDocumentId) {
        KycDocument first = userDocument(userId, firstDocumentId);
        KycDocument second = userDocument(userId, secondDocumentId);
        StringBuilder warning = new StringBuilder();

        if (!safeEquals(first.getOcrName(), second.getOcrName())) {
            warning.append("Name mismatch detected. ");
        }
        if (first.getOcrDob() != null && second.getOcrDob() != null && !first.getOcrDob().equals(second.getOcrDob())) {
            warning.append("DOB mismatch detected. ");
        }
        if (!safeEquals(first.getOcrDocumentNumber(), second.getOcrDocumentNumber())) {
            warning.append("Document number mismatch detected.");
        }

        String result = warning.isEmpty() ? "No mismatches found." : warning.toString().trim();
        first.setComparisonWarning(result);
        second.setComparisonWarning(result);
        documentRepository.save(first);
        documentRepository.save(second);
        return result;
    }

    @Transactional
    public KycDocument rename(Long userId, Long documentId, String documentLabel) {
        KycDocument doc = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        doc.setDocumentName(documentLabel);
        return documentRepository.save(doc);
    }

    @Transactional
    public KycDocument archive(Long userId, Long documentId) {
        KycDocument doc = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        doc.setArchived(true);
        doc.setStatus(DocumentStatus.ARCHIVED);
        return documentRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public KycDocument userDocument(Long userId, Long documentId) {
        return documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }

    @Transactional(readOnly = true)
    public Resource loadAsResource(KycDocument doc, boolean backSide) {
        String fileName = backSide ? doc.getBackFileName() : doc.getFileName();
        if (fileName == null || fileName.isBlank()) {
            throw new RuntimeException("Requested file is not available");
        }
        try {
            Resource resource = new org.springframework.core.io.UrlResource(fileStorageService.getFilePath(fileName).toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("File not found or not readable");
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File path is invalid", ex);
        }
    }

    @Transactional
    public void recordAccess(Long ownerUserId, Long accessorUserId, String accessorEmail, Long documentId, String accessType, String ipAddress, String userAgent) {
        documentAccessLogRepository.save(DocumentAccessLog.builder()
                .ownerUserId(ownerUserId)
                .accessorUserId(accessorUserId)
                .accessorEmail(accessorEmail)
                .documentId(documentId)
                .accessType(accessType)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build());
    }

    private String buildComparisonWarning(IdentityProfile profile, OcrExtractionResult ocr) {
        if (profile == null || ocr == null) {
            return null;
        }
        StringBuilder warning = new StringBuilder();
        if (profile.getFullName() != null && ocr.getExtractedName() != null
                && !profile.getFullName().equalsIgnoreCase(ocr.getExtractedName())) {
            warning.append("OCR name does not match profile name. ");
        }
        if (profile.getDob() != null && ocr.getExtractedDob() != null && !profile.getDob().equals(ocr.getExtractedDob())) {
            warning.append("OCR DOB does not match profile DOB.");
        }
        return warning.isEmpty() ? null : warning.toString().trim();
    }

    private boolean safeEquals(String left, String right) {
        String l = left == null ? "" : left.trim();
        String r = right == null ? "" : right.trim();
        return l.equalsIgnoreCase(r);
    }
}
