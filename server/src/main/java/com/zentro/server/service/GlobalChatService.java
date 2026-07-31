package com.zentro.server.service;

import com.zentro.server.model.GlobalMessage;
import com.zentro.server.model.User;
import com.zentro.server.repository.GlobalMessageRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GlobalChatService {

    private final GlobalMessageRepository globalMessageRepository;

    public GlobalChatService(GlobalMessageRepository globalMessageRepository) {
        this.globalMessageRepository = globalMessageRepository;
    }

    public GlobalMessage sendMessage(User sender, String content) {
        GlobalMessage message = new GlobalMessage();
        message.setSender(sender);
        message.setContent(content);
        return globalMessageRepository.save(message);
    }

    public List<Map<String, Object>> getAllMessages() {
        return globalMessageRepository.findAllGlobalMessages().stream()
                .map(this::sanitizeMessage)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getRecentMessages(int limit) {
        return globalMessageRepository.findRecentMessages(limit).stream()
                .map(this::sanitizeMessage)
                .collect(Collectors.toList());
    }

    public Map<String, Object> sanitizeMessage(GlobalMessage msg) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", msg.getId());
        map.put("content", msg.getContent());
        map.put("timestamp", msg.getTimestamp());

        if (msg.getSender() != null) {
            map.put("senderId", msg.getSender().getId());
            map.put("senderName", msg.getSender().getUsername());
        }

        return map;
    }
}
