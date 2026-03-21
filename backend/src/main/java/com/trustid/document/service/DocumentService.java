package com.trustid.document.service;

import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.trustid.common.enums.DocumentStatus;
import com.trustid.common.enums.DocumentType;
import com.trustid.common.enums.DocumentCategory;
import com.trustid.document.entity.KycDocument;
import com.trustid.document.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public KycDocument uploadDocument(Long userId, MultipartFile file, String name, String type) {
        return uploadDocument(userId, file, name, type, null, null, null, null);
    }

    @Transactional
    public KycDocument uploadDocument(Long userId, MultipartFile file, String name, String type,
                                      String documentNumber, String holderName, String issueDate, String expiryDate) {
        String fileName = fileStorageService.storeFile(file, userId);
        
        KycDocument doc = KycDocument.builder()
                .userId(userId)
                .documentName(name)
                .documentType(DocumentType.valueOf(type.toUpperCase()))
                .documentCategory(DocumentCategory.KYC)
                .fileName(fileName)
                .filePath(fileName) // in local, path is filename relative to upload dir
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .documentNumber(documentNumber)
                .holderName(holderName)
                .status(DocumentStatus.UPLOADED)
                .isArchived(false)
                .isShared(false)
                .uploadedAt(LocalDateTime.now())
                .build();

        if (issueDate != null && !issueDate.isBlank()) {
            doc.setIssueDate(java.time.LocalDate.parse(issueDate));
        }
        if (expiryDate != null && !expiryDate.isBlank()) {
            doc.setExpiryDate(java.time.LocalDate.parse(expiryDate));
        }
                
        return documentRepository.save(doc);
    }
    
    public List<KycDocument> getUserDocuments(Long userId) {
        return documentRepository.findByUserIdAndIsArchivedFalse(userId);
    }

    public KycDocument getUserDocument(Long userId, Long documentId) {
        KycDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!doc.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to access this document");
        }

        return doc;
    }

    public KycDocument getDocumentForAccess(Long requesterUserId, boolean isAdmin, Long documentId) {
        KycDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!isAdmin && !doc.getUserId().equals(requesterUserId)) {
            throw new RuntimeException("Unauthorized to access this document");
        }

        return doc;
    }

    public Resource loadDocumentAsResource(KycDocument document) {
        try {
            Resource resource = new UrlResource(fileStorageService.getFilePath(document.getFileName()).toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("File not found or not readable");
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File path is invalid", ex);
        }
    }

    @Transactional
    public void deleteUserDocument(Long userId, Long documentId) {
        KycDocument doc = getUserDocument(userId, documentId);

        if (doc.isArchived()) {
            return;
        }

        doc.setArchived(true);
        documentRepository.save(doc);
        fileStorageService.deleteFile(doc.getFileName());
    }
}
