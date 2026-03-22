package com.trustid.notification.service;

import com.trustid.common.enums.NotificationType;
import com.trustid.document.entity.KycDocument;
import com.trustid.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiryReminderScheduler {

    private final DocumentRepository documentRepository;
    private final NotificationService notificationService;

    // Daily at 09:00 server time.
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendExpiryReminders() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);

        documentRepository.findAll().stream()
                .filter(doc -> !doc.isArchived())
                .filter(doc -> doc.getExpiryDate() != null)
                .filter(doc -> !doc.getExpiryDate().isAfter(threshold))
                .forEach(this::notifyForExpiry);

        log.debug("Expiry reminder scheduler run completed");
    }

    private void notifyForExpiry(KycDocument doc) {
        String state = doc.getExpiryDate().isBefore(LocalDate.now()) ? "expired" : "expiring soon";
        notificationService.notifyUser(
                doc.getUserId(),
                NotificationType.DOCUMENT_EXPIRY_REMINDER,
                "Document " + state,
                doc.getDocumentName() + " is " + state + " on " + doc.getExpiryDate(),
                null
        );
    }
}
