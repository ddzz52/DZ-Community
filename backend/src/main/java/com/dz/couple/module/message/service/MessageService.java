package com.dz.couple.module.message.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.message.MessageTypes;
import com.dz.couple.module.message.dto.MessageCreateRequest;
import com.dz.couple.module.message.dto.MessageVO;
import com.dz.couple.module.message.entity.MessageCursor;
import com.dz.couple.module.message.mapper.MessageCursorMapper;
import com.dz.couple.module.message.entity.Message;
import com.dz.couple.module.message.mapper.MessageMapper;
import com.dz.couple.module.message.ws.ChatHub;
import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class MessageService {
    private static final long RECALL_WINDOW_MS = 2L * 60L * 1000L;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final ChatHub chatHub;
    private final MessageCursorMapper messageCursorMapper;

    public MessageService(MessageMapper messageMapper, UserMapper userMapper, ChatHub chatHub, MessageCursorMapper messageCursorMapper) {
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.chatHub = chatHub;
        this.messageCursorMapper = messageCursorMapper;
    }

    public List<MessageVO> list(Long userId, Long coupleId, Long beforeId, int limit) {
        ensureMember(userId, coupleId);
        int l = Math.max(1, Math.min(limit, 200));
        List<Message> list = messageMapper.list(coupleId, beforeId, l);
        if (list == null) {
            list = new ArrayList<>();
        }
        Collections.reverse(list);
        List<MessageVO> out = new ArrayList<>();
        for (Message m : list) {
            out.add(toVO(m));
        }
        return out;
    }

    public int countUnread(Long userId, Long coupleId) {
        ensureMember(userId, coupleId);
        return messageMapper.countUnread(coupleId, userId);
    }

    @Transactional
    public MessageVO send(Long userId, Long coupleId, MessageCreateRequest req) {
        ensureMember(userId, coupleId);
        String type = normalizeType(req == null ? null : req.getType());
        String content = safeTrim(req == null ? null : req.getContent());
        if (content == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入内容");
        }
        validateContent(type, content);
        Message m = new Message();
        m.setCoupleId(coupleId);
        m.setSenderId(userId);
        m.setMsgType(type);
        m.setContent(contentForStore(type, content));
        m.setDeliveredAt(null);
        m.setReadAt(null);
        m.setRecalledFlag(0);
        m.setRecalledBy(null);
        m.setRecalledAt(null);
        m.setCreatedAt(new Date());
        Long partnerId = findPartnerId(userId, coupleId);
        if (partnerId != null && chatHub.hasOnline(partnerId)) {
            m.setDeliveredAt(new Date());
        }
        messageMapper.insert(m);
        MessageVO vo = toVO(m);
        if (partnerId != null) {
            chatHub.push(partnerId, new WsEvent("MESSAGE", vo));
        }
        chatHub.push(userId, new WsEvent("MESSAGE", vo));
        return vo;
    }

    public int markReadUpTo(Long userId, Long coupleId, Long upToId) {
        ensureMember(userId, coupleId);
        if (upToId == null) {
            return 0;
        }
        int n = messageMapper.markReadUpTo(coupleId, userId, upToId, new Date());
        if (n > 0) {
            Long partnerId = findPartnerId(userId, coupleId);
            if (partnerId != null) {
                chatHub.push(partnerId, new WsEvent("READ_UP_TO", upToId));
            }
        }
        return n;
    }

    public int markDeliveredUpTo(Long userId, Long coupleId, Long upToId) {
        ensureMember(userId, coupleId);
        if (upToId == null) {
            return 0;
        }
        int n = messageMapper.markDeliveredUpTo(coupleId, userId, upToId, new Date());
        if (n > 0) {
            Long partnerId = findPartnerId(userId, coupleId);
            if (partnerId != null) {
                chatHub.push(partnerId, new WsEvent("DELIVERED_UP_TO", upToId));
            }
        }
        return n;
    }

    @Transactional
    public void delete(Long userId, Long coupleId, Long id) {
        ensureMember(userId, coupleId);
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        int n = messageMapper.deleteById(coupleId, id);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        chatHub.push(userId, new WsEvent("DELETE", id));
        Long partnerId = findPartnerId(userId, coupleId);
        if (partnerId != null) {
            chatHub.push(partnerId, new WsEvent("DELETE", id));
        }
    }

    @Transactional
    public int deleteBatch(Long userId, Long coupleId, List<Long> ids) {
        ensureMember(userId, coupleId);
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int n = messageMapper.deleteBatch(coupleId, ids);
        if (n > 0) {
            chatHub.push(userId, new WsEvent("DELETE_BATCH", ids));
            Long partnerId = findPartnerId(userId, coupleId);
            if (partnerId != null) {
                chatHub.push(partnerId, new WsEvent("DELETE_BATCH", ids));
            }
        }
        return n;
    }

    @Transactional
    public int deleteAll(Long userId, Long coupleId) {
        ensureMember(userId, coupleId);
        int n = messageMapper.deleteAllByCouple(coupleId);
        if (n > 0) {
            chatHub.push(userId, new WsEvent("DELETE_ALL", null));
            Long partnerId = findPartnerId(userId, coupleId);
            if (partnerId != null) {
                chatHub.push(partnerId, new WsEvent("DELETE_ALL", null));
            }
        }
        return n;
    }

    public void recall(Long userId, Long coupleId, Long id) {
        ensureMember(userId, coupleId);
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        Date now = new Date();
        Date cutoff = new Date(now.getTime() - RECALL_WINDOW_MS);
        int n = messageMapper.recall(coupleId, userId, id, cutoff, now);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该消息不可撤回");
        }
        Long partnerId = findPartnerId(userId, coupleId);
        chatHub.push(userId, new WsEvent("RECALL", id));
        if (partnerId != null) {
            chatHub.push(partnerId, new WsEvent("RECALL", id));
        }
    }

    public int ackDelivered(Long userId, Long coupleId, List<Long> ids) {
        ensureMember(userId, coupleId);
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<Message> list = messageMapper.listByIds(coupleId, ids);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (Message m : list) {
            if (m == null || m.getId() == null) {
                continue;
            }
            if (m.getSenderId() == null || m.getSenderId().equals(userId)) {
                continue;
            }
            int u = messageMapper.markDelivered(coupleId, userId, m.getId(), new Date());
            if (u > 0) {
                n += u;
                chatHub.push(m.getSenderId(), new WsEvent("DELIVERED", m.getId()));
            }
        }
        return n;
    }

    public int ackRead(Long userId, Long coupleId, List<Long> ids, String deviceId) {
        ensureMember(userId, coupleId);
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<Message> list = messageMapper.listByIds(coupleId, ids);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int n = 0;
        long maxId = 0;
        for (Message m : list) {
            if (m == null || m.getId() == null) {
                continue;
            }
            if (m.getSenderId() == null || m.getSenderId().equals(userId)) {
                continue;
            }
            int u = messageMapper.markRead(coupleId, userId, m.getId(), new Date());
            if (u > 0) {
                n += u;
                if (m.getId() > maxId) {
                    maxId = m.getId();
                }
                chatHub.push(m.getSenderId(), new WsEvent("READ", m.getId()));
            }
        }
        if (maxId > 0 && deviceId != null && !deviceId.trim().isEmpty()) {
            upsertCursor(userId, coupleId, deviceId.trim(), maxId);
        }
        return n;
    }

    public Long getCursor(Long userId, String deviceId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return null;
        }
        MessageCursor c = messageCursorMapper.find(userId, deviceId.trim());
        return c == null ? null : c.getLastReadId();
    }

    public void upsertCursor(Long userId, Long coupleId, String deviceId, Long lastReadId) {
        ensureMember(userId, coupleId);
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "deviceId不能为空");
        }
        if (lastReadId == null) {
            return;
        }
        MessageCursor c = new MessageCursor();
        c.setUserId(userId);
        c.setCoupleId(coupleId);
        c.setDeviceId(deviceId.trim());
        c.setLastReadId(lastReadId);
        messageCursorMapper.upsert(c);
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId.trim());
        data.put("lastReadId", lastReadId);
        chatHub.push(userId, new WsEvent("CURSOR", data));
    }

    private void ensureMember(Long userId, Long coupleId) {
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        User u = userMapper.findById(userId);
        if (u == null || u.getCoupleId() == null || !u.getCoupleId().equals(coupleId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private Long findPartnerId(Long userId, Long coupleId) {
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

    private MessageVO toVO(Message m) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setSenderId(m.getSenderId());
        vo.setType(m.getMsgType());
        vo.setContent(displayContent(m));
        vo.setDelivered(m.getDeliveredAt() != null);
        vo.setRead(m.getReadAt() != null);
        vo.setRecalled(m.getRecalledFlag() != null && m.getRecalledFlag() == 1);
        vo.setCreatedAt(m.getCreatedAt());
        return vo;
    }

    private String normalizeType(String s) {
        String t = safeTrim(s);
        if (t == null) {
            return MessageTypes.TEXT;
        }
        String u = t.toUpperCase();
        if (MessageTypes.EMOJI.equals(u)) return MessageTypes.EMOJI;
        if (MessageTypes.QUICK.equals(u)) return MessageTypes.QUICK;
        if (MessageTypes.IMAGE.equals(u)) return MessageTypes.IMAGE;
        if (MessageTypes.STICKER.equals(u)) return MessageTypes.STICKER;
        if (MessageTypes.VOICE.equals(u)) return MessageTypes.VOICE;
        return MessageTypes.TEXT;
    }

    private void validateContent(String type, String content) {
        if (content.length() > 500) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "内容长度需≤500");
        }
        if (MessageTypes.EMOJI.equals(type) && content.length() > 32) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "表情内容过长");
        }
        if (MessageTypes.QUICK.equals(type) && content.length() > 80) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "快捷语过长");
        }
        if ((MessageTypes.IMAGE.equals(type) || MessageTypes.STICKER.equals(type) || MessageTypes.VOICE.equals(type))) {
            if (!(content.startsWith("/uploads/") || content.startsWith("http://") || content.startsWith("https://"))) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "媒体地址非法");
            }
        }
    }

    private String contentForStore(String type, String content) {
        if (MessageTypes.TEXT.equals(type) || MessageTypes.EMOJI.equals(type) || MessageTypes.QUICK.equals(type)) {
            return content;
        }
        return content;
    }

    private String displayContent(Message m) {
        if (m == null) {
            return "";
        }
        if (m.getRecalledFlag() != null && m.getRecalledFlag() == 1) {
            return "";
        }
        return m.getContent();
    }

    private String safeTrim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static class WsEvent {
        private String event;
        private Object data;

        public WsEvent() {
        }

        public WsEvent(String event, Object data) {
            this.event = event;
            this.data = data;
        }

        public String getEvent() {
            return event;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }
}
