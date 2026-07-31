package com.zentro.server.service;

import com.zentro.server.model.Message;
import com.zentro.server.model.User;
import com.zentro.server.repository.MessageRepository;
import com.zentro.server.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ActivityLogService logService;

    public ChatService(MessageRepository messageRepository,
                       UserRepository userRepository,
                       ActivityLogService logService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.logService = logService;
    }

    public Message sendMessage(User sender, Long receiverId, String content) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        return messageRepository.save(message);
    }

    public List<Map<String, Object>> getConversation(User user1, Long user2Id) {
        return messageRepository.findConversation(user1.getId(), user2Id).stream()
                .map(this::sanitizeMessage)
                .collect(Collectors.toList());
    }

    public Map<String, Object> markMessagesRead(Long senderId, Long receiverId) {
        messageRepository.markAsRead(senderId, receiverId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    public long getUnreadCount(Long userId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    public List<Map<String, Object>> getConversationsList(User user) {
        List<Message> unread = messageRepository.findUnreadByReceiverId(user.getId());

        Map<Long, Message> lastMessages = new HashMap<>();
        for (Message m : unread) {
            Long senderId = m.getSender().getId();
            if (!lastMessages.containsKey(senderId) ||
                m.getTimestamp().isAfter(lastMessages.get(senderId).getTimestamp())) {
                lastMessages.put(senderId, m);
            }
        }

        return lastMessages.values().stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", m.getSender().getId());
                    map.put("username", m.getSender().getUsername());
                    map.put("lastMessage", m.getContent());
                    map.put("timestamp", m.getTimestamp());
                    map.put("unread", true);
                    return map;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> sanitizeMessage(Message msg) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", msg.getId());
        map.put("content", msg.getContent());
        map.put("isRead", msg.isRead());
        map.put("timestamp", msg.getTimestamp());

        if (msg.getSender() != null) {
            map.put("senderId", msg.getSender().getId());
            map.put("senderName", msg.getSender().getUsername());
        }
        if (msg.getReceiver() != null) {
            map.put("receiverId", msg.getReceiver().getId());
            map.put("receiverName", msg.getReceiver().getUsername());
        }

        return map;
    }
}
