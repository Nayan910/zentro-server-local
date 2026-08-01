package com.zentro.server.service;

import com.zentro.server.model.PrivateMessage;
import com.zentro.server.model.User;
import com.zentro.server.repository.PrivateMessageRepository;
import com.zentro.server.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PrivateChatService {

    private final PrivateMessageRepository privateMessageRepository;
    private final UserRepository userRepository;

    public PrivateChatService(PrivateMessageRepository privateMessageRepository,
                              UserRepository userRepository) {
        this.privateMessageRepository = privateMessageRepository;
        this.userRepository = userRepository;
    }

    public PrivateMessage sendMessage(User sender, Long receiverId, String content) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        PrivateMessage message = new PrivateMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        return privateMessageRepository.save(message);
    }

    public List<Map<String, Object>> getConversation(User user1, Long user2Id) {
        return privateMessageRepository.findConversation(user1.getId(), user2Id).stream()
                .map(this::sanitizeMessage)
                .collect(Collectors.toList());
    }

    public void markMessagesRead(Long senderId, Long receiverId) {
        privateMessageRepository.markAsRead(senderId, receiverId);
    }

    public long getUnreadCount(Long userId) {
        return privateMessageRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    public List<Map<String, Object>> getConversationsList(User user) {
        List<PrivateMessage> allMessages = privateMessageRepository.findAllMessagesByUserId(user.getId());

        Map<Long, PrivateMessage> lastMessages = new HashMap<>();
        for (PrivateMessage m : allMessages) {
            Long partnerId = m.getSender().getId().equals(user.getId())
                    ? m.getReceiver().getId()
                    : m.getSender().getId();

            if (!lastMessages.containsKey(partnerId) ||
                m.getTimestamp().isAfter(lastMessages.get(partnerId).getTimestamp())) {
                lastMessages.put(partnerId, m);
            }
        }

        return lastMessages.entrySet().stream()
                .map(entry -> {
                    PrivateMessage m = entry.getValue();
                    User partner = m.getSender().getId().equals(user.getId())
                            ? m.getReceiver()
                            : m.getSender();

                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", partner.getId());
                    map.put("username", partner.getUsername());
                    map.put("profileImageUrl", partner.getProfileImageUrl());
                    map.put("lastMessage", m.getContent());
                    map.put("timestamp", m.getTimestamp());
                    map.put("isFromMe", m.getSender().getId().equals(user.getId()));
                    return map;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> sanitizeMessage(PrivateMessage msg) {
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
