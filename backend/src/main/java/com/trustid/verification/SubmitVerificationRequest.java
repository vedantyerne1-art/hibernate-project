package com.trustid.verification;

import lombok.Data;
import java.util.List;

@Data
public class SubmitVerificationRequest {
    private List<Long> documentIds;
}
