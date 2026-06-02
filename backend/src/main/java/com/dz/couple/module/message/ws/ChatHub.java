package com.dz.couple.module.message.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class ChatHub {
    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public ChatHub(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(Long userId, WebSocketSession session) {
        if (userId == null || session == null) {
            return;
        }
        sessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void unregister(Long userId, WebSocketSession session) {
        if (userId == null || session == null) {
            return;
        }
        Set<WebSocketSession> set = sessions.get(userId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                sessions.remove(userId);
            }
        }
    }

    public void push(Long userId, Object payload) {
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> set = sessions.get(userId);
        if (set == null || set.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return;
        }
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession s : set) {
            try {
                if (s.isOpen()) {
                    s.sendMessage(msg);
                }
            } catch (Exception e) {
                continue;
            }
        }
    }

    public boolean hasOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        Set<WebSocketSession> set = sessions.get(userId);
        if (set == null || set.isEmpty()) {
            return false;
        }
        for (WebSocketSession s : set) {
            if (s != null && s.isOpen()) {
                return true;
            }
        }
        return false;
    }
}
