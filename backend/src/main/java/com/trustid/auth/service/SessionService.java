package com.trustid.auth.service;

import com.trustid.auth.dto.SessionInfoResponse;
import com.trustid.auth.entity.UserSession;
import com.trustid.auth.repository.UserSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository userSessionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void trackSession(Long userId, String token, String ipAddress, String userAgent, String deviceName) {
        if (token == null || token.isBlank()) {
            return;
        }

        UserSession session = userSessionRepository.findBySessionToken(token)
                .orElse(UserSession.builder()
                        .userId(userId)
                        .sessionToken(token)
                        .build());

        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setDeviceName(deviceName);
        session.setLocation(resolveLocation(ipAddress));
        session.setRevoked(false);
        session.setLastActiveAt(LocalDateTime.now());
        userSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public List<SessionInfoResponse> listActiveSessions(Long userId) {
        return userSessionRepository.findByUserIdAndRevokedFalseOrderByLastActiveAtDesc(userId)
                .stream()
                .map(s -> SessionInfoResponse.builder()
                        .id(s.getId())
                        .sessionToken(s.getSessionToken())
                        .deviceName(s.getDeviceName())
                        .ipAddress(s.getIpAddress())
                        .location(s.getLocation())
                        .userAgent(s.getUserAgent())
                        .lastActiveAt(s.getLastActiveAt())
                        .revoked(s.isRevoked())
                        .build())
                .toList();
    }

    @Transactional
    public void revokeOtherSessions(Long userId, String activeToken) {
        List<UserSession> sessions = userSessionRepository.findByUserIdAndRevokedFalseOrderByLastActiveAtDesc(userId);
        sessions.forEach(session -> {
            if (!session.getSessionToken().equals(activeToken)) {
                session.setRevoked(true);
                userSessionRepository.save(session);
            }
        });
    }

    private String resolveLocation(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return "Unknown";
        }

        String ip = ipAddress.contains(",") ? ipAddress.split(",")[0].trim() : ipAddress.trim();
        if (ip.equals("127.0.0.1") || ip.equals("::1") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
            return "Local Network";
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject("https://ipapi.co/" + ip + "/json/", String.class);
            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            String city = root.path("city").asText("");
            String country = root.path("country_name").asText("");
            String combined = (city + ", " + country).replaceAll("^, |, $", "").trim();
            return combined.isBlank() ? "Unknown" : combined;
        } catch (java.io.IOException | RuntimeException ignored) {
            return "Unknown";
        }
    }
}
