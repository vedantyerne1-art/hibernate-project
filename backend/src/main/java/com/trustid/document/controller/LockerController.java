package com.trustid.document.controller;

import com.trustid.common.dto.ApiResponse;
import com.trustid.common.enums.DocumentType;
import com.trustid.document.dto.LockerDocumentRequest;
import com.trustid.document.dto.RenameDocumentRequest;
import com.trustid.document.entity.KycDocument;
import com.trustid.document.service.LockerService;
import com.trustid.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            @RequestParam(defaultValue = "false") boolean backSide) {
        KycDocument doc = lockerService.userDocument(userDetails.getId(), documentId);
        Resource resource = lockerService.loadAsResource(doc, backSide);
        String contentType = doc.getMimeType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : doc.getMimeType();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (backSide ? doc.getBackFileName() : doc.getFileName()) + "\"")
                .body(resource);
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<Resource> download(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "false") boolean backSide) {
        KycDocument doc = lockerService.userDocument(userDetails.getId(), documentId);
        Resource resource = lockerService.loadAsResource(doc, backSide);
        String contentType = doc.getMimeType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : doc.getMimeType();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + (backSide ? doc.getBackFileName() : doc.getFileName()) + "\"")
                .body(resource);
    }
}
