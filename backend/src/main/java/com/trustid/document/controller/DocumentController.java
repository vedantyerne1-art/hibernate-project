package com.trustid.document.controller;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.trustid.common.dto.ApiResponse;
import com.trustid.document.entity.KycDocument;
import com.trustid.document.service.DocumentService;
import com.trustid.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<KycDocument>> uploadDocument(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("type") String type,
            @RequestParam(value = "documentNumber", required = false) String documentNumber,
            @RequestParam(value = "holderName", required = false) String holderName,
            @RequestParam(value = "issueDate", required = false) String issueDate,
            @RequestParam(value = "expiryDate", required = false) String expiryDate) {
        KycDocument document = documentService.uploadDocument(
                userDetails.getId(), file, name, type, documentNumber, holderName, issueDate, expiryDate);
        return ResponseEntity.ok(ApiResponse.success(document, "Document uploaded successfully"));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<KycDocument>>> getMyDocuments(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<KycDocument> documents = documentService.getUserDocuments(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(documents, "Documents retrieved successfully"));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long documentId) {
        boolean isAdmin = userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))
            || userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        KycDocument document = documentService.getDocumentForAccess(userDetails.getId(), isAdmin, documentId);
        Resource resource = documentService.loadDocumentAsResource(document);

        String contentType = document.getMimeType() != null ? document.getMimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long documentId) {
        documentService.deleteUserDocument(userDetails.getId(), documentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted successfully"));
    }
}
