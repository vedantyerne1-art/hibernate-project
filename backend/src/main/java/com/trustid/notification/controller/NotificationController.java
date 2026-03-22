package com.trustid.notification.controller;

import com.trustid.common.dto.ApiResponse;
import com.trustid.notification.dto.NotificationResponse;
import com.trustid.notification.service.NotificationService;
import com.trustid.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> myNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.myNotifications(userDetails.getId()), "Notifications fetched"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(Map.of("unread", notificationService.unreadCount(userDetails.getId())), "Unread count fetched"));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId) {
        notificationService.markRead(userDetails.getId(), notificationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked as read"));
    }
}
