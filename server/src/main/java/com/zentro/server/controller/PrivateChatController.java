package com.zentro.server.controller;

import com.zentro.server.model.PrivateMessage;
import com.zentro.server.model.User;
import com.zentro.server.service.AuthService;
import com.zentro.server.service.PrivateChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/private")
public class PrivateChatController {

    private final PrivateChatService privateChatService;
    private final AuthService authService;
    private final SimpMessagingTemplate messagingTemplate;

    public PrivateChatController(PrivateChatService privateChatService,
                                  AuthService authService,
                                  SimpMessagingTemplate messagingTemplate) {
        this.privateChatService = privateChatService;
        this.authService = authService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> body,
                                          @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            Long receiverId = Long.parseLong(body.get("receiverId").toString());
            String content = (String) body.get("content");

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Content cannot be empty"));
            }

            PrivateMessage message = privateChatService.sendMessage(user, receiverId, content);
            Map<String, Object> messageData = privateChatService.sanitizeMessage(message);

            messagingTemplate.convertAndSend("/topic/private/" + receiverId, messageData);
            messagingTemplate.convertAndSend("/topic/private/" + user.getId(), messageData);

            return ResponseEntity.ok(Map.of("success", true, "message", messageData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getConversation(@PathVariable Long userId,
                                              @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            List<Map<String, Object>> messages = privateChatService.getConversation(user, userId);
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/read")
    public ResponseEntity<?> markRead(@RequestBody Map<String, Long> body,
                                       @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            Long senderId = body.get("senderId");
            privateChatService.markMessagesRead(senderId, user.getId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/unread/count")
    public ResponseEntity<?> getUnreadCount(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            long count = privateChatService.getUnreadCount(user.getId());
            return ResponseEntity.ok(Map.of("count", count));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = authService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            List<Map<String, Object>> conversations = privateChatService.getConversationsList(user);
            return ResponseEntity.ok(conversations);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
