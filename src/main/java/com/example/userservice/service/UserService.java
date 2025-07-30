package com.example.userservice.service;



import com.example.userservice.dto.LoginRequest;
import com.example.userservice.dto.LoginResponse;
import com.example.userservice.dto.RegisterRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String USER_CACHE_PREFIX = "user:";
    private static final long CACHE_TTL = 3600; // 1 hour

    public UserResponse register(RegisterRequest request) {
        logger.info("Registering new user with username: {}", request.getUsername());

        // Check if user already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            logger.warn("Username already exists: {}", request.getUsername());
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Email already exists: {}", request.getEmail());
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName()
        );

        User savedUser = userRepository.save(user);
        logger.info("Successfully registered user with ID: {}", savedUser.getId());

        // Cache user data (with improved error handling)
        cacheUser(savedUser);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName()
        );
    }

    public LoginResponse login(LoginRequest request) {
        logger.info("Login attempt for username: {}", request.getUsername());

        User user = userRepository.findActiveUserByUsername(request.getUsername())
                .orElseThrow(() -> {
                    logger.warn("User not found: {}", request.getUsername());
                    return new RuntimeException("Invalid username or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Invalid password for user: {}", request.getUsername());
            throw new RuntimeException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        logger.info("Successfully logged in user: {}", request.getUsername());

        // Cache user data (won't fail login if Redis is down)
        cacheUser(user);

        return new LoginResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );
    }

    public UserResponse getUserByUsername(String username) {
        logger.info("Getting user by username: {}", username);

        // Try to get from cache first
        UserResponse cachedUser = getCachedUser(username);
        if (cachedUser != null) {
            logger.info("Retrieved user from cache: {}", username);
            return cachedUser;
        }

        // Get from database
        User user = userRepository.findActiveUserByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("User not found: {}", username);
                    return new RuntimeException("User not found");
                });

        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );

        // Cache the result
        cacheUserResponse(username, userResponse);

        return userResponse;
    }

    public UserResponse updateUser(String username, RegisterRequest request) {
        logger.info("Updating user: {}", username);

        User user = userRepository.findActiveUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if new email is already taken by another user
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
        logger.info("Successfully updated user: {}", username);

        // Update cache
        cacheUser(updatedUser);

        return new UserResponse(
                updatedUser.getId(),
                updatedUser.getUsername(),
                updatedUser.getEmail(),
                updatedUser.getFirstName(),
                updatedUser.getLastName()
        );
    }

    // Improved caching methods with better error handling
    private void cacheUser(User user) {
        try {
            UserResponse userResponse = new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName()
            );

            String cacheKey = USER_CACHE_PREFIX + user.getUsername();
            redisTemplate.opsForValue().set(cacheKey, userResponse, CACHE_TTL, TimeUnit.SECONDS);
            logger.debug("Successfully cached user data for: {}", user.getUsername());
        } catch (Exception e) {
            logger.warn("Failed to cache user data for: {} - Error: {}", user.getUsername(), e.getMessage());
            // Don't throw exception - caching failure shouldn't break business logic
        }
    }

    private void cacheUserResponse(String username, UserResponse userResponse) {
        try {
            String cacheKey = USER_CACHE_PREFIX + username;
            redisTemplate.opsForValue().set(cacheKey, userResponse, CACHE_TTL, TimeUnit.SECONDS);
            logger.debug("Successfully cached user response for: {}", username);
        } catch (Exception e) {
            logger.warn("Failed to cache user response for: {} - Error: {}", username, e.getMessage());
            // Don't throw exception - caching failure shouldn't break business logic
        }
    }

    private UserResponse getCachedUser(String username) {
        try {
            String cacheKey = USER_CACHE_PREFIX + username;
            Object cachedObject = redisTemplate.opsForValue().get(cacheKey);
            if (cachedObject instanceof UserResponse) {
                logger.debug("Successfully retrieved cached user for: {}", username);
                return (UserResponse) cachedObject;
            }
            return null;
        } catch (Exception e) {
            logger.warn("Failed to get cached user for: {} - Error: {}", username, e.getMessage());
            return null; // Fall back to database lookup
        }
    }

    // Add method to clear cache when needed
    public void clearUserCache(String username) {
        try {
            String cacheKey = USER_CACHE_PREFIX + username;
            redisTemplate.delete(cacheKey);
            logger.info("Cleared cache for user: {}", username);
        } catch (Exception e) {
            logger.warn("Failed to clear cache for user: {} - Error: {}", username, e.getMessage());
        }
    }
}