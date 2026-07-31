package com.zentro.server.controller;

import com.zentro.server.model.GlobalMessage;
import com.zentro.server.model.User;
import com.zentro.server.service.AuthService;
import com.zentro.server.service.GlobalChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/global")
public class GlobalChatController {

    private final GlobalChatService globalChatService;
    private final AuthService authService;
    private final SimpMessagingTemplate messagingTemplate;

    public GlobalChatController(GlobalChatService globalChatService,
                                 AuthService authService,
                                 SimpMessagingTemplate messagingTemplate) {
        this.globalChatService = globalChatService;
        this.authService = authService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> body,
                                          @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            String content = body.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Content cannot be empty"));
            }

            GlobalMessage message = globalChatService.sendMessage(user, content);
            Map<String, Object> messageData = globalChatService.sanitizeMessage(message);

            messagingTemplate.convertAndSend("/topic/global", messageData);

            return ResponseEntity.ok(Map.of("success", true, "message", messageData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllMessages(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            List<Map<String, Object>> messages = globalChatService.getAllMessages();
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentMessages(
            @RequestParam(defaultValue = "50") int limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            List<Map<String, Object>> messages = globalChatService.getRecentMessages(limit);
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
