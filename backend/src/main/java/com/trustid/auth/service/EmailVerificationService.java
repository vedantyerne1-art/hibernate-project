package com.trustid.auth.service;

import com.trustid.auth.entity.OtpToken;
import com.trustid.common.enums.OtpType;
import com.trustid.notification.service.MailService;
import com.trustid.user.entity.User;
import com.trustid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final OtpService otpService;
    private final MailService mailService;
    private final UserRepository userRepository;

    public void sendVerificationEmail(String email) {
        OtpToken otpToken = otpService.generateOtp(email, OtpType.EMAIL_VERIFICATION);
        String subject = "TrustID - Verify your email";
        String message = "Your verification OTP is: " + otpToken.getOtpCode() + "\nThis OTP is valid for 10 minutes.";
        mailService.sendEmail(email, subject, message);
    }

    @Transactional
    public boolean verifyEmail(String email, String otp) {
        boolean isValid = otpService.validateOtp(email, otp, OtpType.EMAIL_VERIFICATION);
        if (isValid) {
            User user = userRepository.findByEmail(email).orElseThrow();
            user.setEmailVerified(true);
            userRepository.save(user);
            return true;
        }
        return false;
    }
}
