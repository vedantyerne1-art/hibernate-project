package com.trustid.document.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OcrExtractionResult {
    private String rawText;
    private String extractedName;
    private LocalDate extractedDob;
    private String extractedDocumentNumber;
}
