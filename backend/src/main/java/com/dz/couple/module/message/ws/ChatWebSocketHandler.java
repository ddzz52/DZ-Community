package com.dz.couple.module.message.ws;

import com.dz.couple.module.message.service.MessageService;
import com.dz.couple.module.memo.service.MemoEditLockManager;
import com.dz.couple.module.memo.service.MemoService;
import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final ChatHub chatHub;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final MemoService memoService;

    public ChatWebSocketHandler(ChatHub chatHub, ObjectMapper objectMapper, UserMapper userMapper, MessageService messageService, MemoService memoService) {
        this.chatHub = chatHub;
        this.objectMapper = objectMapper;
        this.userMapper = userMapper;
        this.messageService = messageService;
        this.memoService = memoService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            return;
        }
        chatHub.register(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message == null ? null : message.getPayload();
        if (payload != null && payload.trim().equalsIgnoreCase("ping")) {
            session.sendMessage(new TextMessage("pong"));
            return;
        }
        if (payload == null || payload.trim().isEmpty()) {
            return;
        }
        Map<String, Object> map;
        try {
            map = objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            return;
        }
        if (map == null) {
            return;
        }
        String event = map.get("event") == null ? null : String.valueOf(map.get("event"));
        if (event == null || event.trim().isEmpty()) {
            return;
        }
        if ("TYPING".equalsIgnoreCase(event)) {
            Object v = map.get("typing");
            boolean typing = v instanceof Boolean ? (Boolean) v : "1".equals(String.valueOf(v)) || "true".equalsIgnoreCase(String.valueOf(v));
            Long userId = (Long) session.getAttributes().get("userId");
            Long coupleId = (Long) session.getAttributes().get("coupleId");
            Long partnerId = findPartnerId(userId, coupleId);
            if (partnerId == null) {
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("from", userId);
            data.put("typing", typing);
            chatHub.push(partnerId, new MessageService.WsEvent("TYPING", data));
            return;
        }
        if ("DELIVERED".equalsIgnoreCase(event) || "READ".equalsIgnoreCase(event)) {
            Long userId = (Long) session.getAttributes().get("userId");
            Long coupleId = (Long) session.getAttributes().get("coupleId");
            Long id = null;
            Object v = map.get("id");
            if (v != null) {
                try {
                    id = Long.parseLong(String.valueOf(v));
                } catch (Exception e) {
                    id = null;
                }
            }
            if (id == null) {
                return;
            }
            List<Long> ids = java.util.Collections.singletonList(id);
            if ("DELIVERED".equalsIgnoreCase(event)) {
                messageService.ackDelivered(userId, coupleId, ids);
            } else {
                String deviceId = map.get("deviceId") == null ? null : String.valueOf(map.get("deviceId"));
                if (deviceId == null || deviceId.trim().isEmpty()) {
                    Object dv = session.getAttributes().get("deviceId");
                    deviceId = dv == null ? null : String.valueOf(dv);
                }
                messageService.ackRead(userId, coupleId, ids, deviceId);
            }
            return;
        }
        if ("DELIVERED_BATCH".equalsIgnoreCase(event) || "READ_BATCH".equalsIgnoreCase(event)) {
            Long userId = (Long) session.getAttributes().get("userId");
            Long coupleId = (Long) session.getAttributes().get("coupleId");
            Object v = map.get("ids");
            if (!(v instanceof List)) {
                return;
            }
            List idsAny = (List) v;
            List<Long> ids = new java.util.ArrayList<>();
            for (Object it : idsAny) {
                if (it == null) continue;
                try {
                    ids.add(Long.parseLong(String.valueOf(it)));
                } catch (Exception e) {
                    continue;
                }
            }
            if (ids.isEmpty()) {
                return;
            }
            if ("DELIVERED_BATCH".equalsIgnoreCase(event)) {
                messageService.ackDelivered(userId, coupleId, ids);
            } else {
                String deviceId = map.get("deviceId") == null ? null : String.valueOf(map.get("deviceId"));
                if (deviceId == null || deviceId.trim().isEmpty()) {
                    Object dv = session.getAttributes().get("deviceId");
                    deviceId = dv == null ? null : String.valueOf(dv);
                }
                messageService.ackRead(userId, coupleId, ids, deviceId);
            }
            return;
        }
        if ("CURSOR".equalsIgnoreCase(event)) {
            Long userId = (Long) session.getAttributes().get("userId");
            Long coupleId = (Long) session.getAttributes().get("coupleId");
            String deviceId = map.get("deviceId") == null ? null : String.valueOf(map.get("deviceId"));
            if (deviceId == null || deviceId.trim().isEmpty()) {
                Object dv = session.getAttributes().get("deviceId");
                deviceId = dv == null ? null : String.valueOf(dv);
            }
            Long lastReadId = null;
            Object v = map.get("lastReadId");
            if (v != null) {
                try {
                    lastReadId = Long.parseLong(String.valueOf(v));
                } catch (Exception e) {
                    lastReadId = null;
                }
            }
            if (lastReadId != null) {
                messageService.upsertCursor(userId, coupleId, deviceId, lastReadId);
            }
            return;
        }
        if ("MEMO_EDIT_START".equalsIgnoreCase(event) || "MEMO_EDIT_END".equalsIgnoreCase(event) || "MEMO_EDIT_HEARTBEAT".equalsIgnoreCase(event)) {
            Long userId = (Long) session.getAttributes().get("userId");
            Long coupleId = (Long) session.getAttributes().get("coupleId");
            Long id = null;
            Object v = map.get("id");
            if (v != null) {
                try {
                    id = Long.parseLong(String.valueOf(v));
                } catch (Exception e) {
                    id = null;
                }
            }
            if (id == null) {
                return;
            }
            if ("MEMO_EDIT_HEARTBEAT".equalsIgnoreCase(event)) {
                memoService.heartbeatEditing(userId, coupleId, id);
                return;
            }
            if ("MEMO_EDIT_END".equalsIgnoreCase(event)) {
                memoService.endEditing(userId, coupleId, id);
                return;
            }
            MemoEditLockManager.TryLockResult r = memoService.tryStartEditing(userId, coupleId, id);
            if (r != null && r.isOk()) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", id);
                sendSession(session, "MEMO_EDIT_OK", data);
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            MemoEditLockManager.LockInfo lock = r == null ? null : r.getLockedBy();
            if (lock != null) {
                data.put("byUserId", lock.getUserId());
                data.put("byNickname", lock.getNickname());
            }
            sendSession(session, "MEMO_EDIT_DENIED", data);
            return;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        chatHub.unregister(userId, session);
    }

    private Long findPartnerId(Long userId, Long coupleId) {
        if (userId == null || coupleId == null) {
            return null;
        }
        List<User> list = userMapper.listByCoupleId(coupleId);
        if (list == null) {
            return null;
        }
        for (User u : list) {
            if (u != null && u.getId() != null && !u.getId().equals(userId)) {
                return u.getId();
            }
        }
        return null;
    }

    private void sendSession(WebSocketSession session, String event, Object data) {
        if (session == null || event == null || event.trim().isEmpty()) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", event);
        payload.put("data", data);
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception e) {
            return;
        }
    }
}
