package com.trustid.developer;

import com.trustid.common.enums.NotificationType;
import com.trustid.developer.dto.ShareLinkRequest;
import com.trustid.document.entity.DocumentAccessLog;
import com.trustid.document.entity.KycDocument;
import com.trustid.document.repository.DocumentAccessLogRepository;
import com.trustid.document.repository.DocumentRepository;
import com.trustid.notification.service.NotificationService;
import com.trustid.user.entity.User;
import com.trustid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentAccessLogRepository accessLogRepository;
    private final NotificationService notificationService;

    @Value("${app.share.public-base-url:http://localhost:5173/share}")
    private String shareBaseUrl;

    @Transactional
    public String createShareLink(String ownerEmail, ShareLinkRequest request) {
        User owner = userRepository.findByEmail(ownerEmail).orElseThrow(() -> new RuntimeException("User not found"));
        if (request.getDocumentIds() == null || request.getDocumentIds().isEmpty()) {
            throw new RuntimeException("Select at least one document to share");
        }

        List<KycDocument> docs = documentRepository.findAllById(request.getDocumentIds());
        docs.forEach(d -> {
            if (!d.getUserId().equals(owner.getId())) {
                throw new RuntimeException("Cannot share documents you do not own");
            }
            d.setShared(true);
        });
        documentRepository.saveAll(docs);

        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().substring(0, 6);
        ShareLink link = ShareLink.builder()
                .ownerUserId(owner.getId())
                .token(token)
                .documentIdsCsv(request.getDocumentIds().stream().map(String::valueOf).collect(Collectors.joining(",")))
                .expiresAt(LocalDateTime.now().plusMinutes(Math.max(request.getExpiresInMinutes() == null ? 60 : request.getExpiresInMinutes(), 5)))
                .revoked(false)
                .build();
        shareLinkRepository.save(link);

        notificationService.notifyUser(owner.getId(), NotificationType.SHARE_LINK_CREATED, "Share link created", "Your secure document share link is ready.", null);
        return shareBaseUrl + "/" + token;
    }

    @Transactional(readOnly = true)
    public List<KycDocument> resolveShareLink(String token, String accessorEmail, String ipAddress, String userAgent) {
        ShareLink link = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid share link"));

        if (link.isRevoked() || link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Share link expired or revoked");
        }

        List<Long> ids = Arrays.stream(link.getDocumentIdsCsv().split(","))
                .filter(v -> !v.isBlank())
                .map(Long::parseLong)
                .toList();

        List<KycDocument> docs = documentRepository.findAllById(ids);
        docs.forEach(doc -> accessLogRepository.save(DocumentAccessLog.builder()
                .ownerUserId(link.getOwnerUserId())
                .accessorEmail(accessorEmail)
                .documentId(doc.getId())
                .accessType("CONSENT_SHARE_LINK_ACCESS")
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build()));
        return docs;
    }

    @Transactional
    public void revokeLink(String ownerEmail, Long shareLinkId) {
        User owner = userRepository.findByEmail(ownerEmail).orElseThrow(() -> new RuntimeException("User not found"));
        ShareLink link = shareLinkRepository.findById(shareLinkId).orElseThrow(() -> new RuntimeException("Share link not found"));
        if (!link.getOwnerUserId().equals(owner.getId())) {
            throw new RuntimeException("Unauthorized share-link revoke");
        }
        link.setRevoked(true);
        shareLinkRepository.save(link);
        notificationService.notifyUser(owner.getId(), NotificationType.SHARE_LINK_REVOKED, "Share link revoked", "A share link has been revoked.", null);
    }
}
