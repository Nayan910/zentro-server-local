package com.zentro.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zentro.server.model.Message;
import com.zentro.server.model.User;
import com.zentro.server.repository.UserRepository;
import com.zentro.server.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    // userId -> WebSocketSession
    private final ConcurrentHashMap<Long, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatWebSocketHandler(ChatService chatService, UserRepository userRepository) {
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            activeSessions.put(userId, session);
            logger.info("WebSocket connected: userId={}", userId);

            // Send connection confirmation
            Map<String, Object> confirmation = Map.of(
                    "type", "connected",
                    "userId", userId
            );
            sendMessage(session, objectMapper.writeValueAsString(confirmation));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = getUserIdFromSession(session);
        if (senderId == null) return;

        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");

            if ("chat".equals(type)) {
                handleChatMessage(senderId, payload);
            } else if ("typing".equals(type)) {
                handleTypingNotification(senderId, payload);
            } else if ("read".equals(type)) {
                handleReadReceipt(senderId, payload);
            }
        } catch (Exception e) {
            logger.error("Error handling WebSocket message", e);
            Map<String, Object> error = Map.of("type", "error", "message", "Invalid message format");
            sendMessage(session, objectMapper.writeValueAsString(error));
        }
    }

    private void handleChatMessage(Long senderId, Map<String, Object> payload) throws Exception {
        Long receiverId = Long.parseLong(payload.get("receiverId").toString());
        String content = (String) payload.get("content");

        User sender = userRepository.findById(senderId).orElse(null);
        if (sender == null) return;

        // Save to database
        Message savedMessage = chatService.sendMessage(sender, receiverId, content);

        // Build response
        Map<String, Object> response = Map.of(
                "type", "chat",
                "id", savedMessage.getId(),
                "senderId", senderId,
                "senderName", sender.getUsername(),
                "receiverId", receiverId,
                "content", content,
                "timestamp", savedMessage.getTimestamp().toString(),
                "isRead", false
        );

        String responseJson = objectMapper.writeValueAsString(response);

        // Send to receiver if online
        WebSocketSession receiverSession = activeSessions.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            sendMessage(receiverSession, responseJson);
        }

        // Send confirmation back to sender
        WebSocketSession senderSession = activeSessions.get(senderId);
        if (senderSession != null && senderSession.isOpen()) {
            Map<String, Object> confirmation = Map.of(
                    "type", "sent",
                    "id", savedMessage.getId(),
                    "timestamp", savedMessage.getTimestamp().toString()
            );
            sendMessage(senderSession, objectMapper.writeValueAsString(confirmation));
        }
    }

    private void handleTypingNotification(Long senderId, Map<String, Object> payload) throws Exception {
        Long receiverId = Long.parseLong(payload.get("receiverId").toString());

        WebSocketSession receiverSession = activeSessions.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            User sender = userRepository.findById(senderId).orElse(null);
            if (sender != null) {
                Map<String, Object> notification = Map.of(
                        "type", "typing",
                        "userId", senderId,
                        "username", sender.getUsername()
                );
                sendMessage(receiverSession, objectMapper.writeValueAsString(notification));
            }
        }
    }

    private void handleReadReceipt(Long senderId, Map<String, Object> payload) throws Exception {
        Long chatPartnerId = Long.parseLong(payload.get("userId").toString());
        chatService.markMessagesRead(chatPartnerId, senderId);

        // Notify the chat partner that messages were read
        WebSocketSession partnerSession = activeSessions.get(chatPartnerId);
        if (partnerSession != null && partnerSession.isOpen()) {
            Map<String, Object> receipt = Map.of(
                    "type", "read",
                    "userId", senderId
            );
            sendMessage(partnerSession, objectMapper.writeValueAsString(receipt));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            activeSessions.remove(userId);
            logger.info("WebSocket disconnected: userId={}", userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            activeSessions.remove(userId);
            logger.error("WebSocket transport error: userId={}", userId, exception);
        }
    }

    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
            }
        } catch (IOException e) {
            logger.error("Error sending WebSocket message", e);
        }
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        try {
            String uri = session.getUri().toString();
            // URL format: /ws/chat?token=XXX
            String query = session.getUri().getQuery();
            if (query != null && query.contains("token=")) {
                String token = query.replace("token=", "");
                // We'll use a simple approach: extract userId from token
                // In production, validate the JWT here
                String[] parts = token.split("\\.");
                if (parts.length == 3) {
                    // JWT has 3 parts: header.payload.signature
                    String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                    Map<String, Object> claims = objectMapper.readValue(payload, Map.class);
                    return Long.parseLong(claims.get("sub").toString());
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting userId from session", e);
        }
        return null;
    }
}
