package com.trustid.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendVerificationRequest {
    @Email
    @NotBlank
    private String email;
}