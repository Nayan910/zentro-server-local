package com.zentro.server.service;

import com.zentro.server.model.User;
import com.zentro.server.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<Map<String, Object>> getAllWorkers() {
        return userRepository.findAll().stream()
                .filter(u -> "worker".equals(u.getRole()))
                .map(this::sanitizeUser)
                .collect(Collectors.toList());
    }

    public User updateUser(Long id, Map<String, Object> updates) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return null;

        if (updates.containsKey("phone")) user.setPhone((String) updates.get("phone"));
        if (updates.containsKey("location")) user.setLocation((String) updates.get("location"));
        if (updates.containsKey("tradeCategory")) user.setTradeCategory((String) updates.get("tradeCategory"));
        if (updates.containsKey("experienceYears")) user.setExperienceYears((Integer) updates.get("experienceYears"));
        if (updates.containsKey("isAvailable")) user.setAvailable((Boolean) updates.get("isAvailable"));
        if (updates.containsKey("role")) user.setRole((String) updates.get("role"));
        if (updates.containsKey("profileImageUrl")) user.setProfileImageUrl((String) updates.get("profileImageUrl"));
        if (updates.containsKey("rating")) user.setRating((Double) updates.get("rating"));
        if (updates.containsKey("name")) user.setUsername((String) updates.get("name"));

        return userRepository.save(user);
    }

    public List<Map<String, Object>> searchUsers(String query) {
        String lowerQuery = query.toLowerCase();
        return userRepository.findAll().stream()
                .filter(u -> (u.getUsername() != null && u.getUsername().toLowerCase().contains(lowerQuery)) ||
                             (u.getTradeCategory() != null && u.getTradeCategory().toLowerCase().contains(lowerQuery)) ||
                             (u.getLocation() != null && u.getLocation().toLowerCase().contains(lowerQuery)))
                .map(this::sanitizeUser)
                .collect(Collectors.toList());
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
        return map;
    }
}
