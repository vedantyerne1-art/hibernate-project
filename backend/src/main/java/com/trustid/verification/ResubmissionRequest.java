package com.trustid.verification;

import lombok.Data;

import java.util.List;

@Data
public class ResubmissionRequest {
    private List<Long> documentIds;
}
