package com.trustid.auth.dto;

import com.trustid.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    private Long id;
    private String fullName;
    private String email;
    private Role role;
    private boolean emailVerified;
    private boolean verificationEmailSent;
}
