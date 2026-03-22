package com.trustid.document.controller;

import com.trustid.common.dto.ApiResponse;
import com.trustid.common.enums.DocumentCategory;
import com.trustid.common.enums.DocumentType;
import com.trustid.document.dto.LockerDocumentRequest;
import com.trustid.document.dto.RenameDocumentRequest;
import com.trustid.document.entity.DocumentFolder;
import com.trustid.document.entity.KycDocument;
import com.trustid.document.service.LockerService;
import com.trustid.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/locker")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class LockerController {

    private final LockerService lockerService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<KycDocument>> upload(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("frontFile") MultipartFile frontFile,
            @RequestPart(value = "backFile", required = false) MultipartFile backFile,
            @RequestPart("metadata") String metadataJson) {
        try {
            LockerDocumentRequest request = objectMapper.readValue(metadataJson, LockerDocumentRequest.class);
            KycDocument doc = lockerService.upload(userDetails.getId(), frontFile, backFile, request);
            return ResponseEntity.ok(ApiResponse.success(doc, "Document uploaded to locker"));
        } catch (JsonProcessingException | RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<KycDocument>>> list(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) DocumentType type,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean archived) {
        return ResponseEntity.ok(ApiResponse.success(lockerService.list(userDetails.getId(), type, q, archived), "Locker documents fetched"));
    }

    @GetMapping("/documents/category/{category}")
    public ResponseEntity<ApiResponse<List<KycDocument>>> listByCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable DocumentCategory category) {
        return ResponseEntity.ok(ApiResponse.success(lockerService.listByCategory(userDetails.getId(), category), "Category documents fetched"));
    }

    @GetMapping("/documents/{documentId}/versions")
    public ResponseEntity<ApiResponse<List<KycDocument>>> versions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long documentId) {
        return ResponseEntity.ok(ApiResponse.success(lockerService.versionHistory(userDetails.getId(), documentId), "Document version history fetched"));
    }

    @GetMapping("/documents/expiring")
    public ResponseEntity<ApiResponse<List<KycDocument>>> expiring(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "30") int withinDays) {
        return ResponseEntity.ok(ApiResponse.success(lockerService.expiringDocuments(userDetails.getId(), withinDays), "Expiring documents fetched"));
    }

    @GetMapping("/documents/compare")
    public ResponseEntity<ApiResponse<Map<String, String>>> compare(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long first,
            @RequestParam Long second) {
        String result = lockerService.compareDocuments(userDetails.getId(), first, second);
        return ResponseEntity.ok(ApiResponse.success(Map.of("result", result), "Document comparison completed"));
    }

    @PostMapping("/folders")
    public ResponseEntity<ApiResponse<DocumentFolder>> createFolder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, Object> payload) {
        String folderName = payload.get("folderName") == null ? null : String.valueOf(payload.get("folderName"));
        Long parentFolderId = payload.get("parentFolderId") == null ? null : Long.valueOf(String.valueOf(payload.get("parentFolderId")));
        return ResponseEntity.ok(ApiResponse.success(lockerService.createFolder(userDetails.getId(), folderName, parentFolderId), "Folder created"));
    }

    @GetMapping("/folders")
    public ResponseEntity<ApiResponse<List<DocumentFolder>>> listFolders(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(lockerService.listFolders(userDetails.getId()), "Folders fetched"));
    }

    @PatchMapping("/documents/{documentId}/rename")
    public ResponseEntity<ApiResponse<KycDocument>> rename(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long documentId,
            @RequestBody RenameDocumentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(lockerService.rename(userDetails.getId(), documentId, request.getDocumentLabel()), "Document renamed"));
    }

    @PatchMapping("/documents/{documentId}/archive")
    public ResponseEntity<ApiResponse<KycDocument>> archive(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long documentId) {
        return ResponseEntity.ok(ApiResponse.success(lockerService.archive(userDetails.getId(), documentId), "Document archived"));
    }

    @GetMapping("/documents/{documentId}/preview")
    public ResponseEntity<Resource> preview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "false") boolean backSide,
            HttpServletRequest request) {
        KycDocument doc = lockerService.userDocument(userDetails.getId(), documentId);
        Resource resource = lockerService.loadAsResource(doc, backSide);
        String contentType = doc.getMimeType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : doc.getMimeType();
        lockerService.recordAccess(
                doc.getUserId(),
                userDetails.getId(),
                userDetails.getUsername(),
                documentId,
                "PREVIEW",
                request.getRemoteAddr(),
                request.getHeader("User-Agent"));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (backSide ? doc.getBackFileName() : doc.getFileName()) + "\"")
                .body(resource);
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<Resource> download(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "false") boolean backSide,
            HttpServletRequest request) {
        KycDocument doc = lockerService.userDocument(userDetails.getId(), documentId);
        Resource resource = lockerService.loadAsResource(doc, backSide);
        String contentType = doc.getMimeType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : doc.getMimeType();
        lockerService.recordAccess(
                doc.getUserId(),
                userDetails.getId(),
                userDetails.getUsername(),
                documentId,
                "DOWNLOAD",
                request.getRemoteAddr(),
                request.getHeader("User-Agent"));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + (backSide ? doc.getBackFileName() : doc.getFileName()) + "\"")
                .body(resource);
    }
}
