package com.trustid.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.trustid.common.enums.Role;
import com.trustid.user.entity.User;
import com.trustid.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@Profile("dev")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin-bootstrap.enabled:true}")
    private boolean enabled;

    @Value("${app.admin-bootstrap.full-name:Dev Admin}")
    private String adminFullName;

    @Value("${app.admin-bootstrap.email:admin@trustid.local}")
    private String adminEmail;

    @Value("${app.admin-bootstrap.password:Admin@12345}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!enabled || userRepository.existsByEmail(adminEmail)) {
            return;
        }

        User admin = User.builder()
                .fullName(adminFullName)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .emailVerified(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build();

        userRepository.save(admin);
    }
}
