package com.trustid.document.dto;

import com.trustid.common.enums.DocumentCategory;
import com.trustid.common.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LockerDocumentRequest {
    private DocumentType documentType;
    private DocumentCategory documentCategory;
    private String documentLabel;
    private String documentNumber;
    private String holderName;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String folderName;
    private String tags;
}
