package com.trustid.identity.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimelineEventResponse {
    private String eventType;
    private String description;
    private LocalDateTime occurredAt;
}
