package com.hieu.notification_service.service;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends HTML emails via JavaMailSender (Mailhog in dev, real SMTP in prod).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Sends a single HTML email. Throws {@link RuntimeException} on SMTP failure
     * so callers can mark the notification as FAILED.
     */
    public void send(String to, String subject, String htmlBody) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.setFrom("noreply@notification-service");
            mailSender.send(message);
            log.info("Email sent to={} subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to={}: {}", to, e.getMessage());
            throw new RuntimeException("Email send failed: " + e.getMessage(), e);
        }
    }
}
