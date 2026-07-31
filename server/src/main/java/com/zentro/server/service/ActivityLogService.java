package com.zentro.server.service;

import com.zentro.server.model.ActivityLog;
import com.zentro.server.model.User;
import com.zentro.server.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {

    private final ActivityLogRepository logRepository;

    public ActivityLogService(ActivityLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public void log(User user, String action, String details, String ipAddress) {
        ActivityLog log = new ActivityLog();
        log.setUser(user);
        log.setAction(action);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        logRepository.save(log);
    }

    public List<Map<String, Object>> getAllLogs() {
        return logRepository.findAllByOrderByTimestampDesc().stream()
                .map(this::sanitizeLog)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getLogsByUser(Long userId) {
        return logRepository.findByUserIdOrderByTimestampDesc(userId).stream()
                .map(this::sanitizeLog)
                .collect(Collectors.toList());
    }

    private Map<String, Object> sanitizeLog(ActivityLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getId());
        map.put("action", log.getAction());
        map.put("details", log.getDetails());
        map.put("ipAddress", log.getIpAddress());
        map.put("timestamp", log.getTimestamp());

        if (log.getUser() != null) {
            map.put("userId", log.getUser().getId());
            map.put("username", log.getUser().getUsername());
        }

        return map;
    }
}
