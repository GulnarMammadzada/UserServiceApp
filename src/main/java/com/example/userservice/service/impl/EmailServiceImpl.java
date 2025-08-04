package com.example.userservice.service.impl;

import com.example.userservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${email.template.welcome.subject}")
    private String welcomeSubject;

    @Value("${email.template.profile-update.subject}")
    private String profileUpdateSubject;

    @Value("${email.template.admin-promotion.subject}")
    private String adminPromotionSubject;

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            javaMailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email sending error: " + e.getMessage());
        }
    }

    @Override
    public void sendWelcomeEmail(String to, String firstName, String lastName) {
        String body = String.format(
                "Dear %s %s,\n\n" +
                        "Welcome to our system!\n\n" +
                        "Your registration has been completed successfully. You can now use all our services.\n\n" +
                        "If you have any questions, feel free to contact us.\n\n" +
                        "Best regards,\n" +
                        "The Team",
                firstName, lastName
        );

        sendEmail(to, welcomeSubject, body);
    }

    @Override
    public void sendProfileUpdateEmail(String to, String firstName, String lastName) {
        String body = String.format(
                "Dear %s %s,\n\n" +
                        "Your profile information has been updated successfully.\n\n" +
                        "If you did not make this change, please contact us immediately.\n\n" +
                        "Best regards,\n" +
                        "The Team",
                firstName, lastName
        );

        sendEmail(to, profileUpdateSubject, body);
    }

    @Override
    public void sendAdminPromotionEmail(String to, String firstName, String lastName) {
        String body = String.format(
                "Dear %s %s,\n\n" +
                        "Congratulations! You have been granted admin privileges.\n\n" +
                        "You can now access all administrative functions as a system administrator.\n\n" +
                        "Best regards,\n" +
                        "The Team",
                firstName, lastName
        );

        sendEmail(to, adminPromotionSubject, body);
    }
}