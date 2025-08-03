package com.example.userservice.config;

import com.example.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TokenCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupScheduler.class);

    @Autowired
    private UserService userService;

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredTokens() {
        logger.info("Starting cleanup of expired refresh tokens");
        try {
            userService.cleanupExpiredTokens();
            logger.info("Successfully cleaned up expired refresh tokens");
        } catch (Exception e) {
            logger.error("Failed to cleanup expired tokens", e);
        }
    }
}