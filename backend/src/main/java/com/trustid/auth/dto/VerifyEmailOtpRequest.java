package com.trustid.auth.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
public class VerifyEmailOtpRequest {
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String otp;
}
