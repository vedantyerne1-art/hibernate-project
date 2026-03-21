package com.trustid.document.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RenameDocumentRequest {
    @NotBlank
    private String documentLabel;
}
