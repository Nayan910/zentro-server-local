package com.zentro.server.controller;

import com.zentro.server.service.ActivityLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final ActivityLogService logService;

    public LogController(ActivityLogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllLogs() {
        return ResponseEntity.ok(logService.getAllLogs());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getLogsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(logService.getLogsByUser(userId));
    }
}
