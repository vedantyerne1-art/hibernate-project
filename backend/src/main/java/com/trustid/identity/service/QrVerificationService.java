package com.trustid.identity.service;

import java.net.MalformedURLException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import com.trustid.document.service.FileStorageService;
import com.trustid.identity.dto.PublicQrVerificationResponse;
import com.trustid.identity.dto.QrTokenResponse;
import com.trustid.identity.entity.IdentityProfile;
import com.trustid.identity.repository.IdentityRepository;
import com.trustid.security.JwtService;
import com.trustid.user.entity.User;
import com.trustid.user.repository.UserRepository;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class QrVerificationService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final IdentityRepository identityRepository;
        private final FileStorageService fileStorageService;

    @Value("${app.qr.token-expiration-ms:300000}")
    private long qrTokenExpirationMs;

    @Value("${app.qr.verify-base-url:http://localhost:5173/public/verify}")
    private String qrVerifyBaseUrl;

    public QrTokenResponse generateForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        IdentityProfile profile = identityRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Identity profile not found"));

        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", user.getId());
        claims.put("fullName", profile.getFullName());
        claims.put("identityNumber", profile.getIdentityNumber());
        claims.put("status", profile.getStatus().name());

        String token = jwtService.generateToken(claims, "qr-verify:" + user.getEmail(), qrTokenExpirationMs);
        LocalDateTime expiresAt = LocalDateTime.now().plusNanos(qrTokenExpirationMs * 1_000_000L);
        String resolvedVerifyBaseUrl = resolvePublicBaseUrl(qrVerifyBaseUrl);

        return QrTokenResponse.builder()
                .token(token)
                .verificationUrl(resolvedVerifyBaseUrl + "?token=" + token)
                .expiresAt(expiresAt)
                .build();
    }

    public PublicQrVerificationResponse resolve(String token) {
                Claims claims = jwtService.extractClaims(token);
                IdentityProfile profile = resolveProfile(claims);

        return PublicQrVerificationResponse.builder()
                .valid(true)
                .verified("APPROVED".equalsIgnoreCase(profile.getStatus().name()))
                .fullName(profile.getFullName())
                .identityNumber(profile.getIdentityNumber())
                                .profilePhotoUrl(profile.getProfilePhotoUrl())
                .status(profile.getStatus().name())
                .issueDate(profile.getApprovedAt() != null ? profile.getApprovedAt().toLocalDate() : null)
                .expiresAt(LocalDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneId.systemDefault()))
                .build();
    }

        public Resource loadProfilePhoto(String token) {
                Claims claims = jwtService.extractClaims(token);
                IdentityProfile profile = resolveProfile(claims);

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

        private IdentityProfile resolveProfile(Claims claims) {
                Long userId = claims.get("uid", Number.class).longValue();
                return identityRepository.findByUserId(userId)
                                .orElseThrow(() -> new RuntimeException("Identity profile not found"));
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
