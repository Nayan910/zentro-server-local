package com.zentro.server.service;

import com.zentro.server.model.User;
import com.zentro.server.repository.UserRepository;
import com.zentro.server.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ActivityLogService logService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       ActivityLogService logService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.logService = logService;
    }

    public Map<String, Object> register(String username, String email, String password,
                                         String phone, String role, String location,
                                         String tradeCategory, String ipAddress) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setRole(role != null ? role : "worker");
        user.setLocation(location);
        user.setTradeCategory(tradeCategory);
        user.setAvailable(true);
        user.setRating(4.0);

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        logService.log(user, "REGISTER", "New user registered: " + username, ipAddress);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", sanitizeUser(user));
        return response;
    }

    public Map<String, Object> login(String username, String password, String ipAddress) {
        Optional<User> optUser = userRepository.findByUsername(username);
        if (optUser.isEmpty()) {
            throw new RuntimeException("Invalid username or password");
        }

        User user = optUser.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        logService.log(user, "LOGIN", "User logged in: " + username, ipAddress);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", sanitizeUser(user));
        return response;
    }

    public User getUserFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || !jwtUtil.validateToken(token)) {
            return null;
        }
        Long userId = jwtUtil.extractUserId(token);
        return userRepository.findById(userId).orElse(null);
    }

    private Map<String, Object> sanitizeUser(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("phone", user.getPhone());
        map.put("role", user.getRole());
        map.put("location", user.getLocation());
        map.put("tradeCategory", user.getTradeCategory());
        map.put("experienceYears", user.getExperienceYears());
        map.put("rating", user.getRating());
        map.put("reviewCount", user.getReviewCount());
        map.put("profileImageUrl", user.getProfileImageUrl());
        map.put("isAvailable", user.isAvailable());
        map.put("isVerified", user.isVerified());
        map.put("createdAt", user.getCreatedAt());
        map.put("updatedAt", user.getUpdatedAt());
        return map;
    }
}
