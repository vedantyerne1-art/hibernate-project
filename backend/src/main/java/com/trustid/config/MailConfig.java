package com.trustid.config;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

@Configuration
public class MailConfig {

    @Bean
    @Primary
    public JavaMailSender javaMailSender(
            @Value("${GMAIL_USERNAME:}") String gmailUsername,
            @Value("${GMAIL_APP_PASSWORD:}") String gmailAppPassword
    ) {
        if (!StringUtils.hasText(gmailUsername) || !StringUtils.hasText(gmailAppPassword)) {
            throw new IllegalStateException("Gmail SMTP is required. Set GMAIL_USERNAME and GMAIL_APP_PASSWORD.");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        Properties props = sender.getJavaMailProperties();

        sender.setHost("smtp.gmail.com");
        sender.setPort(587);
        sender.setUsername(gmailUsername);
        sender.setPassword(gmailAppPassword);

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return sender;
    }
}
