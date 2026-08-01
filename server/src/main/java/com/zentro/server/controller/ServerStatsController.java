package com.zentro.server.controller;

import com.zentro.server.config.RequestLoggingFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/server")
public class ServerStatsController {

    private final RequestLoggingFilter loggingFilter;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final long startTime = System.currentTimeMillis();

    public ServerStatsController(RequestLoggingFilter loggingFilter) {
        this.loggingFilter = loggingFilter;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();

        Map<String, Object> stats = new HashMap<>();
        stats.put("status", "RUNNING");
        stats.put("timestamp", LocalDateTime.now().format(formatter));
        stats.put("uptime", formatUptime(System.currentTimeMillis() - startTime));

        // Request stats
        Map<String, Object> requestStats = new HashMap<>();
        requestStats.put("totalRequests", loggingFilter.getRequestCount());
        requestStats.put("totalBytesIn", loggingFilter.getTotalBytesIn());
        requestStats.put("totalBytesOut", loggingFilter.getTotalBytesOut());
        stats.put("requests", requestStats);

        // Memory stats
        Map<String, Object> memoryStats = new HashMap<>();
        memoryStats.put("heapUsed", formatBytes(memory.getHeapMemoryUsage().getUsed()));
        memoryStats.put("heapMax", formatBytes(memory.getHeapMemoryUsage().getMax()));
        memoryStats.put("nonHeapUsed", formatBytes(memory.getNonHeapMemoryUsage().getUsed()));
        stats.put("memory", memoryStats);

        // System info
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("processors", Runtime.getRuntime().availableProcessors());
        systemInfo.put("freeMemory", formatBytes(Runtime.getRuntime().freeMemory()));
        stats.put("system", systemInfo);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().format(formatter));
        return ResponseEntity.ok((Map) health);
    }

    private String formatUptime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        if (hours > 0) return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
