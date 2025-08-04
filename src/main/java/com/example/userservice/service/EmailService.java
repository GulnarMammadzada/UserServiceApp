package com.example.userservice.service;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
    void sendWelcomeEmail(String to, String firstName, String lastName);
    void sendProfileUpdateEmail(String to, String firstName, String lastName);
    void sendAdminPromotionEmail(String to, String firstName, String lastName);
}