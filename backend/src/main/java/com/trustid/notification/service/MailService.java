package com.trustid.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${GMAIL_USERNAME:${spring.mail.username:}}")
    private String mailFrom;

    @Value("${app.mail.fallback-to-mailpit:true}")
    private boolean fallbackToMailpit;

    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        if (mailFrom != null && !mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }
        try {
            mailSender.send(message);
        } catch (MailAuthenticationException ex) {
            if (!fallbackToMailpit) {
                throw ex;
            }
            sendViaMailpitFallback(message);
        }
    }

    private void sendViaMailpitFallback(SimpleMailMessage original) {
        JavaMailSenderImpl fallbackSender = new JavaMailSenderImpl();
        fallbackSender.setHost("localhost");
        fallbackSender.setPort(1025);

        SimpleMailMessage fallbackMessage = new SimpleMailMessage();
        fallbackMessage.setTo(original.getTo());
        fallbackMessage.setSubject(original.getSubject());
        fallbackMessage.setText(original.getText());
        fallbackMessage.setFrom("no-reply@trustid.local");

        fallbackSender.send(fallbackMessage);
    }
}
