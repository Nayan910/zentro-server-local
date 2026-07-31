package com.zentro.server.controller;

import com.zentro.server.model.User;
import com.zentro.server.repository.UserRepository;
import com.zentro.server.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserSearchController {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserSearchController(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String username,
                                          @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            List<User> users = userRepository.findByUsernameContainingIgnoreCase(username);
            List<Map<String, Object>> userList = users.stream()
                    .map(this::sanitizeUser)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(userList);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username,
                                                @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            return userRepository.findByUsername(username)
                    .map(u -> ResponseEntity.ok((Object) sanitizeUser(u)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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
        return map;
    }
}
