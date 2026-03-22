package com.trustid.auth.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionInfoResponse {
    private Long id;
    private String sessionToken;
    private String deviceName;
    private String ipAddress;
    private String location;
    private String userAgent;
    private LocalDateTime lastActiveAt;
    private boolean revoked;
}
