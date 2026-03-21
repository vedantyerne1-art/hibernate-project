package com.trustid.document.service;

import com.trustid.common.enums.DocumentCategory;
import com.trustid.common.enums.DocumentStatus;
import com.trustid.common.enums.DocumentType;
import com.trustid.document.dto.LockerDocumentRequest;
import com.trustid.document.entity.KycDocument;
import com.trustid.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class LockerService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public KycDocument upload(Long userId, MultipartFile frontFile, MultipartFile backFile, LockerDocumentRequest request) {
        if (frontFile == null || frontFile.isEmpty()) {
            throw new RuntimeException("Front file is required");
        }

        String frontName = fileStorageService.storeFile(frontFile, userId);
        String backName = (backFile != null && !backFile.isEmpty()) ? fileStorageService.storeFile(backFile, userId) : null;

        KycDocument doc = KycDocument.builder()
                .userId(userId)
                .documentType(request.getDocumentType() == null ? DocumentType.OTHER : request.getDocumentType())
                .documentCategory(request.getDocumentCategory() == null ? DocumentCategory.KYC : request.getDocumentCategory())
                .documentName((request.getDocumentLabel() == null || request.getDocumentLabel().isBlank()) ? "Untitled Document" : request.getDocumentLabel())
                .documentNumber(request.getDocumentNumber())
                .holderName(request.getHolderName())
                .issueDate(request.getIssueDate())
                .expiryDate(request.getExpiryDate())
                .fileName(frontName)
                .filePath(frontName)
                .backFileName(backName)
                .backFilePath(backName)
                .mimeType(frontFile.getContentType())
                .fileSize(frontFile.getSize())
                .status(DocumentStatus.UPLOADED)
                .isArchived(false)
                .isShared(false)
                .uploadedAt(LocalDateTime.now())
                .build();

        return documentRepository.save(doc);
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
}
