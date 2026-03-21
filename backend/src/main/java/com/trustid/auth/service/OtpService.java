package com.trustid.auth.service;

import com.trustid.auth.entity.OtpToken;
import com.trustid.auth.repository.OtpTokenRepository;
import com.trustid.common.enums.OtpType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private static final int OTP_VALIDITY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    @Transactional
    public OtpToken generateOtp(String email, OtpType type) {
        String code = String.format("%06d", new Random().nextInt(999999));
        
        OtpToken token = OtpToken.builder()
                .email(email)
                .otpCode(code)
                .otpType(type)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES))
                .used(false)
                .attemptCount(0)
                .resendCount(0)
                .build();
                
        return otpTokenRepository.save(token);
    }

    @Transactional
    public boolean validateOtp(String email, String code, OtpType type) {
        Optional<OtpToken> tokenOpt = otpTokenRepository.findTopByEmailAndOtpTypeOrderByCreatedAtDesc(email, type);
        
        if (tokenOpt.isEmpty()) return false;
        
        OtpToken token = tokenOpt.get();
        
        if (token.isUsed() || token.getExpiresAt().isBefore(LocalDateTime.now()) || token.getAttemptCount() >= MAX_ATTEMPTS) {
            return false;
        }

        token.setAttemptCount(token.getAttemptCount() + 1);
        
        if (token.getOtpCode().equals(code)) {
            token.setUsed(true);
            otpTokenRepository.save(token);
            return true;
        }
        
        otpTokenRepository.save(token);
        return false;
    }
}
