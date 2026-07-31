package com.zentro.server.controller;

import com.zentro.server.model.User;
import com.zentro.server.service.AuthService;
import com.zentro.server.service.GigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gigs")
public class GigController {

    private final GigService gigService;
    private final AuthService authService;

    public GigController(GigService gigService, AuthService authService) {
        this.gigService = gigService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<?> createGig(@RequestBody Map<String, Object> body,
                                        @RequestHeader(value = "Authorization", required = false) String authHeader,
                                        HttpServletRequest request) {
        try {
            User user = authService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            String title = (String) body.get("title");
            String description = (String) body.get("description");
            double budget = body.get("budget") != null ?
                    Double.parseDouble(body.get("budget").toString()) : 0;
            String location = (String) body.get("location");
            String tradeCategory = (String) body.get("tradeCategory");
            String deadline = (String) body.get("deadline");
            String ip = request.getRemoteAddr();

            Map<String, Object> result = gigService.createGig(
                    user, title, description, budget, location, tradeCategory, deadline, ip);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllGigs() {
        return ResponseEntity.ok(gigService.getAllOpenGigs());
    }

    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getMyGigs(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = authService.getUserFromToken(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(gigService.getMyGigs(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGigById(@PathVariable Long id) {
        Map<String, Object> gig = gigService.getGigById(id);
        if (gig == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(gig);
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<?> applyForGig(@PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          @RequestHeader(value = "Authorization", required = false) String authHeader,
                                          HttpServletRequest request) {
        try {
            User user = authService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            String message = body.get("message");
            String ip = request.getRemoteAddr();

            Map<String, Object> result = gigService.applyForGig(user, id, message, ip);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{gigId}/applications/{applicationId}/accept")
    public ResponseEntity<?> acceptApplication(
            @PathVariable Long gigId,
            @PathVariable Long applicationId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {
        try {
            User user = authService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            String ip = request.getRemoteAddr();
            Map<String, Object> result = gigService.acceptApplication(user, applicationId, ip);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/applications")
    public ResponseEntity<List<Map<String, Object>>> getGigApplications(@PathVariable Long id) {
        return ResponseEntity.ok(gigService.getGigApplications(id));
    }
}
