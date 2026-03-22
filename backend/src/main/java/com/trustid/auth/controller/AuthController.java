package com.trustid.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trustid.auth.dto.AuthResponse;
import com.trustid.auth.dto.LoginRequest;
import com.trustid.auth.dto.RegisterRequest;
import com.trustid.auth.dto.SendVerificationRequest;
import com.trustid.auth.dto.VerifyEmailOtpRequest;
import com.trustid.auth.service.AuthService;
import com.trustid.auth.service.EmailVerificationService;
import com.trustid.common.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.register(request, extractClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
            return ResponseEntity.ok(ApiResponse.success(response, "User registered successfully, please verify email."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.login(request, extractClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
            return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid email or password"));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping("/send-verification")
    public ResponseEntity<ApiResponse<Void>> sendVerification(@Valid @RequestBody SendVerificationRequest request) {
        try {
            emailVerificationService.sendVerificationEmail(request.getEmail());
            return ResponseEntity.ok(ApiResponse.success(null, "Verification OTP sent"));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailOtpRequest request) {
        boolean isVerified = emailVerificationService.verifyEmail(request.getEmail(), request.getOtp());
        if (isVerified) {
            return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired OTP"));
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
