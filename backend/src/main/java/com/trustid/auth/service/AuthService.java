package com.trustid.auth.service;

import com.trustid.auth.dto.AuthResponse;
import com.trustid.auth.dto.LoginRequest;
import com.trustid.auth.dto.RegisterRequest;
import com.trustid.audit.service.AuditService;
import com.trustid.common.enums.AuditAction;
import com.trustid.common.enums.Role;
import com.trustid.security.CustomUserDetails;
import com.trustid.security.JwtService;
import com.trustid.user.entity.User;
import com.trustid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationService emailVerificationService;
        private final SessionService sessionService;
        private final AuditService auditService;

    @Transactional
        public AuthResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .emailVerified(false)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build();

        User savedUser = userRepository.save(user);
                boolean verificationEmailSent = true;
                try {
                        emailVerificationService.sendVerificationEmail(savedUser.getEmail());
                } catch (RuntimeException ex) {
                        verificationEmailSent = false;
                }
        
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String jwtToken = jwtService.generateToken(userDetails);
        sessionService.trackSession(savedUser.getId(), jwtToken, ipAddress, userAgent, resolveDeviceName(userAgent));
        auditService.log(savedUser.getId(), savedUser.getRole(), AuditAction.REGISTER, "USER", savedUser.getId(), "User registered", ipAddress, userAgent);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .emailVerified(savedUser.isEmailVerified())
                .verificationEmailSent(verificationEmailSent)
                .build();
    }

    @Transactional
        public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Reset failed login attempts on success
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String jwtToken = jwtService.generateToken(userDetails);
        sessionService.trackSession(user.getId(), jwtToken, ipAddress, userAgent, resolveDeviceName(userAgent));
        auditService.log(user.getId(), user.getRole(), AuditAction.LOGIN_SUCCESS, "USER", user.getId(), "User logged in", ipAddress, userAgent);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .verificationEmailSent(true)
                .build();
    }

        private String resolveDeviceName(String userAgent) {
                if (userAgent == null || userAgent.isBlank()) {
                        return "Unknown Device";
                }
                String ua = userAgent.toLowerCase();
                if (ua.contains("android")) return "Android";
                if (ua.contains("iphone") || ua.contains("ios")) return "iPhone";
                if (ua.contains("windows")) return "Windows";
                if (ua.contains("mac")) return "Mac";
                if (ua.contains("linux")) return "Linux";
                return "Web Browser";
        }
}
