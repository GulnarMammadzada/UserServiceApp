package com.example.userservice.service;

import com.example.userservice.dto.*;
import com.example.userservice.entity.RefreshToken;
import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import com.example.userservice.repository.RefreshTokenRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        logger.info("Registering new user with username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Use request.getRole() if provided, otherwise default to USER
        Role role = request.getRole() != null ? request.getRole() : Role.USER;

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName(),
                role
        );

        User savedUser = userRepository.save(user);

        // Send welcome email
        try {
            emailService.sendWelcomeEmail(
                    savedUser.getEmail(),
                    savedUser.getFirstName(),
                    savedUser.getLastName()
            );
            logger.info("Welcome email sent successfully to: {}", savedUser.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send welcome email to {}: {}", savedUser.getEmail(), e.getMessage());
            // Email xətası qeydiyyatı dayandırmasın
        }

        return mapToUserResponse(savedUser);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        logger.info("Login attempt for username: {}", request.getUsername());

        User user = userRepository.findActiveUserByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshTokenString = jwtUtil.generateRefreshTokenString();

        // Save refresh token
        RefreshToken refreshToken = new RefreshToken(
                refreshTokenString,
                user.getUsername(),
                LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000)
        );
        refreshTokenRepository.save(refreshToken);

        return new LoginResponse(
                accessToken,
                refreshTokenString,
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndIsUsedFalse(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        User user = userRepository.findActiveUserByUsername(refreshToken.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Mark old token as used
        refreshToken.setIsUsed(true);
        refreshTokenRepository.save(refreshToken);

        // Generate new tokens
        String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole().name());
        String newRefreshTokenString = jwtUtil.generateRefreshTokenString();

        RefreshToken newRefreshToken = new RefreshToken(
                newRefreshTokenString,
                user.getUsername(),
                LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000)
        );
        refreshTokenRepository.save(newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshTokenString);
    }

    @Transactional
    public void logout(String username) {
        refreshTokenRepository.markAllTokensAsUsedForUser(username);
    }

    // Admin Functions
    @Transactional
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToUserResponse);
    }

    @Transactional
    public Page<UserResponse> getUsersByRole(Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable).map(this::mapToUserResponse);
    }

    @Transactional
    public Page<UserResponse> searchUsers(String searchTerm, Pageable pageable) {
        return userRepository.searchUsers(searchTerm, pageable).map(this::mapToUserResponse);
    }

    @Transactional
    public UserResponse updateUserAsAdmin(Long userId, AdminUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getEmail() != null && !user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getIsActive() != null) user.setIsActive(request.getIsActive());

        User updatedUser = userRepository.save(user);

        return mapToUserResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsActive(false);
        userRepository.save(user);
        refreshTokenRepository.deleteAllTokensForUser(user.getUsername());
    }

    @Transactional
    public UserResponse promoteToAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(Role.ADMIN);
        User updatedUser = userRepository.save(user);

        // Send admin promotion email
        try {
            emailService.sendAdminPromotionEmail(
                    updatedUser.getEmail(),
                    updatedUser.getFirstName(),
                    updatedUser.getLastName()
            );
            logger.info("Admin promotion email sent successfully to: {}", updatedUser.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send admin promotion email to {}: {}", updatedUser.getEmail(), e.getMessage());
        }

        return mapToUserResponse(updatedUser);
    }

    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findActiveUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(String username, RegisterRequest request) {
        User user = userRepository.findActiveUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);

        // Send profile update email
        try {
            emailService.sendProfileUpdateEmail(
                    updatedUser.getEmail(),
                    updatedUser.getFirstName(),
                    updatedUser.getLastName()
            );
            logger.info("Profile update email sent successfully to: {}", updatedUser.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send profile update email to {}: {}", updatedUser.getEmail(), e.getMessage());
        }

        return mapToUserResponse(updatedUser);
    }

    // Helper methods
    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getIsActive()
        );
    }

    // Cleanup expired tokens
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}