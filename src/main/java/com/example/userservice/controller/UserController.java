package com.example.userservice.controller;

import com.example.userservice.dto.*;
import com.example.userservice.entity.Role;
import com.example.userservice.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:8080")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    // Auth endpoints
    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            logger.info("Registration request received for username: {}", request.getUsername());
            UserResponse user = userService.register(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("user", user);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Registration failed for username: {}", request.getUsername(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            logger.info("Login request received for username: {}", request.getUsername());
            LoginResponse loginResponse = userService.login(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("data", loginResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Login failed for username: {}", request.getUsername(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            TokenResponse tokenResponse = userService.refreshToken(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", tokenResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Token refresh failed", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            userService.logout(username);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logout successful");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Logout failed", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // User profile endpoints
    @GetMapping("/users/profile")
    public ResponseEntity<?> getUserProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            logger.info("Profile request received for username: {}", username);
            UserResponse user = userService.getUserByUsername(username);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", user);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get profile for username: {}", username, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PutMapping("/users/profile")
    public ResponseEntity<?> updateUserProfile(@Valid @RequestBody RegisterRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            logger.info("Profile update request received for username: {}", username);
            UserResponse user = userService.updateUser(username, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("user", user);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to update profile for username: {}", username, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            logger.info("Get user request received for username: {}", username);
            UserResponse user = userService.getUserByUsername(username);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", user);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get user by username: {}", username, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    // Admin endpoints
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<UserResponse> users = userService.getAllUsers(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("users", users.getContent());
            response.put("currentPage", users.getNumber());
            response.put("totalItems", users.getTotalElements());
            response.put("totalPages", users.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get all users", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/admin/users/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsersByRole(
            @PathVariable Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<UserResponse> users = userService.getUsersByRole(role, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("users", users.getContent());
            response.put("currentPage", users.getNumber());
            response.put("totalItems", users.getTotalElements());
            response.put("totalPages", users.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get users by role: {}", role, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/admin/users/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> searchUsers(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<UserResponse> users = userService.searchUsers(searchTerm, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("users", users.getContent());
            response.put("currentPage", users.getNumber());
            response.put("totalItems", users.getTotalElements());
            response.put("totalPages", users.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to search users with term: {}", searchTerm, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserAsAdmin(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserRequest request) {
        try {
            logger.info("Admin update request for user ID: {}", userId);
            UserResponse user = userService.updateUserAsAdmin(userId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User updated successfully");
            response.put("user", user);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to update user ID: {}", userId, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/admin/users/{userId}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> promoteToAdmin(@PathVariable Long userId) {
        try {
            logger.info("Promoting user ID to admin: {}", userId);
            UserResponse user = userService.promoteToAdmin(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User promoted to admin successfully");
            response.put("user", user);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to promote user ID: {}", userId, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @DeleteMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        try {
            logger.info("Deactivating user ID: {}", userId);
            userService.deleteUser(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User deactivated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to deactivate user ID: {}", userId, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

}