package com.trustid.identity.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.trustid.identity.dto.DigitalIdCardResponse;
import com.trustid.identity.dto.PublicQrVerificationResponse;
import com.trustid.identity.entity.IdentityProfile;
import com.trustid.identity.entity.VerificationToken;
import com.trustid.identity.repository.IdentityRepository;
import com.trustid.identity.repository.VerificationTokenRepository;
import com.trustid.user.entity.User;
import com.trustid.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DigitalIdService {

    private final UserRepository userRepository;
    private final IdentityRepository identityRepository;
    private final VerificationTokenRepository verificationTokenRepository;

    @Value("${app.qr.verify-base-url:http://localhost:5173/public/verify}")
    private String verifyBaseUrl;

    @Value("${app.qr.token-expiration-ms:300000}")
    private long tokenExpirationMs;

    @Transactional
    public DigitalIdCardResponse generateDigitalId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        IdentityProfile profile = identityRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Identity profile not found"));

        if (!"APPROVED".equals(profile.getStatus().name())) {
            throw new RuntimeException("Identity profile is not approved yet");
        }

        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime expiresAt = LocalDateTime.now().plusNanos(tokenExpirationMs * 1_000_000L);

        VerificationToken vt = VerificationToken.builder()
                .token(token)
                .userId(user.getId())
                .identityProfileId(profile.getId())
                .expiresAt(expiresAt)
                .used(false)
                .build();
        verificationTokenRepository.save(vt);

        String verificationUrl = verifyBaseUrl + "/" + token;

        return DigitalIdCardResponse.builder()
                .fullName(profile.getFullName())
                .identityNumber(profile.getIdentityNumber())
                .profilePhotoUrl(profile.getProfilePhotoUrl())
                .dob(profile.getDob())
                .status("VERIFIED")
                .issueDate(profile.getApprovedAt() != null ? profile.getApprovedAt().toLocalDate() : LocalDate.now())
                .token(token)
                .verificationUrl(verificationUrl)
                .qrCodeBase64(generateQrBase64(verificationUrl))
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional(readOnly = true)
    public PublicQrVerificationResponse verifyPublicToken(String token) {
        VerificationToken vt = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (vt.isUsed() || vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token is expired or invalid");
        }

        IdentityProfile profile = identityRepository.findById(vt.getIdentityProfileId())
                .orElseThrow(() -> new RuntimeException("Identity profile not found"));

        return PublicQrVerificationResponse.builder()
                .valid(true)
                .verified("APPROVED".equals(profile.getStatus().name()))
                .fullName(profile.getFullName())
                .identityNumber(profile.getIdentityNumber())
            .profilePhotoUrl(profile.getProfilePhotoUrl())
                .status(profile.getStatus().name())
                .issueDate(profile.getApprovedAt() != null ? profile.getApprovedAt().toLocalDate() : null)
                .expiresAt(vt.getExpiresAt())
                .build();
    }

    private String generateQrBase64(String value) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(value, BarcodeFormat.QR_CODE, 240, 240);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (WriterException | java.io.IOException ex) {
            // Keep card generation resilient even if QR encoding fails.
            return "data:text/plain;base64," + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
