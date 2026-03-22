package com.trustid.developer.dto;

import lombok.Data;

import java.util.List;

@Data
public class ShareLinkRequest {
    private List<Long> documentIds;
    private Integer expiresInMinutes;
}
