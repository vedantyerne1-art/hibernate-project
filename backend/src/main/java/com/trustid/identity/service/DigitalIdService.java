package com.trustid.identity.service;

import java.io.ByteArrayOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Enumeration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.trustid.document.service.FileStorageService;
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
    private final FileStorageService fileStorageService;

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

            String resolvedVerifyBaseUrl = resolvePublicBaseUrl(verifyBaseUrl);
            String verificationUrl = resolvedVerifyBaseUrl + "/" + token;

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

    @Transactional(readOnly = true)
    public Resource loadPublicTokenProfilePhoto(String token) {
        VerificationToken vt = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (vt.isUsed() || vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token is expired or invalid");
        }

        IdentityProfile profile = identityRepository.findById(vt.getIdentityProfileId())
                .orElseThrow(() -> new RuntimeException("Identity profile not found"));

        String fileName = profile.getProfilePhotoUrl();
        if (fileName == null || fileName.isBlank()) {
            throw new RuntimeException("Profile photo not found");
        }

        try {
            Resource resource = new UrlResource(fileStorageService.getFilePath(fileName).toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Profile photo not found", ex);
        }

        throw new RuntimeException("Profile photo not found");
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

    private String resolvePublicBaseUrl(String configuredBaseUrl) {
        if (configuredBaseUrl == null || configuredBaseUrl.isBlank()) {
            return configuredBaseUrl;
        }

        if (!configuredBaseUrl.contains("localhost") && !configuredBaseUrl.contains("127.0.0.1")) {
            return configuredBaseUrl;
        }

        String lanIp = detectLanIpv4();
        if (lanIp == null || lanIp.isBlank()) {
            return configuredBaseUrl;
        }

        return configuredBaseUrl
                .replace("localhost", lanIp)
                .replace("127.0.0.1", lanIp);
    }

    private String detectLanIpv4() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            return null;
        }

        return null;
    }
}
