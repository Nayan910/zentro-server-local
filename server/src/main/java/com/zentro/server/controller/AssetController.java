package com.zentro.server.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private static final String ASSETS_DIR = "assets";

    @GetMapping("/{path:.+}")
    public ResponseEntity<?> getAsset(@PathVariable String path) {
        try {
            Path assetPath = Paths.get(ASSETS_DIR, path).normalize();

            // Security check - prevent path traversal
            if (!assetPath.startsWith(ASSETS_DIR)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid path");
                return ResponseEntity.badRequest().body(error);
            }

            File file = assetPath.toFile();
            if (!file.exists()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Asset not found");
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = determineContentType(path);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .header("Cache-Control", "max-age=3600")
                    .body(resource);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to load asset: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadAsset(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                         @RequestParam(value = "path", defaultValue = "") String path) {
        try {
            if (file.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "File is empty");
                return ResponseEntity.badRequest().body(error);
            }

            String filename = file.getOriginalFilename();
            Path targetPath = Paths.get(ASSETS_DIR, path, filename).normalize();

            // Security check
            if (!targetPath.startsWith(ASSETS_DIR)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid path");
                return ResponseEntity.badRequest().body(error);
            }

            // Create directories if needed
            Files.createDirectories(targetPath.getParent());

            // Save file
            file.transferTo(targetPath.toFile());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("path", targetPath.toString());
            result.put("size", file.getSize());
            result.put("url", "/api/assets/" + Paths.get(path, filename).toString().replace("\\", "/"));

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/list/{path:.+}")
    public ResponseEntity<?> listAssets(@PathVariable String path) {
        try {
            Path dirPath = Paths.get(ASSETS_DIR, path).normalize();

            if (!dirPath.startsWith(ASSETS_DIR)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid path");
                return ResponseEntity.badRequest().body(error);
            }

            File dir = dirPath.toFile();
            if (!dir.exists() || !dir.isDirectory()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Directory not found");
                return ResponseEntity.notFound().build();
            }

            File[] files = dir.listFiles();
            if (files == null) files = new File[0];

            java.util.List<Map<String, Object>> fileList = new java.util.ArrayList<>();
            for (File file : files) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("name", file.getName());
                fileInfo.put("isDirectory", file.isDirectory());
                fileInfo.put("size", file.length());
                fileInfo.put("lastModified", file.lastModified());
                fileList.add(fileInfo);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("path", path);
            result.put("files", fileList);
            result.put("count", fileList.size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to list assets: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    private String determineContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".xml")) return "application/xml";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        return "application/octet-stream";
    }
}
