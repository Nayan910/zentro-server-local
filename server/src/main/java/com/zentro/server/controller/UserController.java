package com.zentro.server.controller;

import com.zentro.server.model.User;
import com.zentro.server.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.getUserById(Long.parseLong(userDetails.getUsername()));
            if (user == null) {
                // Fallback: try parsing as the actual user ID from JWT
                return ResponseEntity.ok(Map.of("error", "User not found"));
            }
            return ResponseEntity.ok(sanitizeUser(user));
        } catch (Exception e) {
            // This shouldn't happen with proper JWT, but handle gracefully
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Could not retrieve user");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sanitizeUser(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                         @RequestBody Map<String, Object> body) {
        try {
            User updated = userService.updateUser(id, body);
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(sanitizeUser(updated));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllWorkers());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(@RequestParam String q) {
        return ResponseEntity.ok(userService.searchUsers(q));
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
