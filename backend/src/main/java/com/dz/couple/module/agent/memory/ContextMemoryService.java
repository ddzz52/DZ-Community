package com.dz.couple.module.agent.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis 上下文记忆管理器
 * 存储当前对话的短期上下文，对话结束后 1 小时过期
 */
@Service
public class ContextMemoryService {

    private static final Logger log = LoggerFactory.getLogger(ContextMemoryService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String CONTEXT_PREFIX = "agent:context:";
    private static final String DIALOGUE_PREFIX = "agent:dialogue:";
    
    // 上下文过期时间：1 小时
    private static final long CONTEXT_EXPIRE_HOURS = 1;
    
    // 对话历史保留条数
    private static final int MAX_DIALOGUE_COUNT = 50;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 获取用户的 Redis key
     */
    private String getContextKey(Long userId) {
        return CONTEXT_PREFIX + userId;
    }

    private String getDialogueKey(Long userId) {
        return DIALOGUE_PREFIX + userId;
    }

    /**
     * 保存对话上下文 — 只保留真正的对话，过滤工具执行结果
     */
    public void saveContext(Long userId, String userMessage, String agentResponse,
                           String intent, List<String> actions) {
        if (userId == null) {
            return;
        }

        // 过滤：工具执行类意图不保存上下文（天气数据、查询结果等会污染后续对话）
        if (isToolIntent(intent)) {
            log.debug("跳过工具类意图上下文保存: userId={}, intent={}", userId, intent);
            return;
        }

        String contextKey = getContextKey(userId);
        String dialogueKey = getDialogueKey(userId);

        try {
            // 截断 agent 回复，避免长文本占满上下文
            String truncatedResponse = agentResponse != null && agentResponse.length() > 300
                    ? agentResponse.substring(0, 300) + "..." : agentResponse;

            Map<String, Object> dialogue = new HashMap<>();
            dialogue.put("userMessage", userMessage);
            dialogue.put("agentResponse", truncatedResponse);
            dialogue.put("intent", intent);
            dialogue.put("timestamp", System.currentTimeMillis());

            String dialogueJson = JSON.writeValueAsString(dialogue);
            redisTemplate.opsForList().leftPush(dialogueKey, dialogueJson);
            redisTemplate.opsForList().trim(dialogueKey, 0, MAX_DIALOGUE_COUNT - 1);
            redisTemplate.expire(dialogueKey, CONTEXT_EXPIRE_HOURS, TimeUnit.HOURS);

            Map<String, Object> context = new HashMap<>();
            context.put("lastUserMessage", userMessage);
            context.put("lastAgentResponse", truncatedResponse);
            context.put("lastIntent", intent);
            context.put("lastUpdateTime", System.currentTimeMillis());

            String contextJson = JSON.writeValueAsString(context);
            redisTemplate.opsForValue().set(contextKey, contextJson, CONTEXT_EXPIRE_HOURS, TimeUnit.HOURS);

            log.debug("保存上下文记忆: userId={}, intent={}", userId, intent);
        } catch (Exception e) {
            log.error("保存上下文记忆失败：userId={}", userId, e);
        }
    }

    /** 判断是否为工具执行类意图（数据查询返回不保存为上下文） */
    private boolean isToolIntent(String intent) {
        if (intent == null) return true;
        return intent.startsWith("QUERY_") || intent.startsWith("ACCOUNT_")
            || intent.startsWith("CREATE_") || intent.startsWith("ADD_")
            || intent.contains("WEATHER") || intent.contains("TRAVEL") || intent.contains("DATE")
            || intent.contains("PERIOD") || intent.contains("DASHBOARD") || intent.contains("TIMELINE");
    }

    /**
     * 获取当前对话上下文（最后一次交互）
     */
    public Map<String, Object> getCurrentContext(Long userId) {
        if (userId == null) {
            return new HashMap<>();
        }

        String contextKey = getContextKey(userId);
        String json = redisTemplate.opsForValue().get(contextKey);
        
        if (json == null) {
            return new HashMap<>();
        }

        try {
            return JSON.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("解析上下文失败：userId={}", userId, e);
            return new HashMap<>();
        }
    }

    /**
     * 获取最近对话历史
     */
    public List<Map<String, Object>> getRecentDialogues(Long userId, int count) {
        if (userId == null) {
            return new ArrayList<>();
        }

        String dialogueKey = getDialogueKey(userId);
        List<String> dialogues = redisTemplate.opsForList().range(dialogueKey, 0, count - 1);
        
        if (dialogues == null || dialogues.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String json : dialogues) {
            try {
                result.add(JSON.readValue(json, new TypeReference<Map<String, Object>>() {}));
            } catch (Exception e) {
                log.warn("解析对话历史失败", e);
            }
        }

        return result;
    }

    /**
     * 构建上下文文本（用于 Prompt）— 取最近3轮有效对话，去除工具调用痕迹
     */
    public String buildContextText(Long userId) {
        List<Map<String, Object>> dialogues = getRecentDialogues(userId, 10);
        if (dialogues.isEmpty()) {
            return "";
        }

        // 过滤出真正对话轮次，只取最近3轮
        List<Map<String, Object>> chatTurns = new ArrayList<>();
        for (Map<String, Object> d : dialogues) {
            String intent = (String) d.get("intent");
            if (intent != null && isConversationalIntent(intent)) {
                chatTurns.add(d);
                if (chatTurns.size() >= 3) break;
            }
        }

        if (chatTurns.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("【最近对话】\n");
        for (int i = chatTurns.size() - 1; i >= 0; i--) {
            Map<String, Object> d = chatTurns.get(i);
            sb.append("- 用户: ").append(d.get("userMessage")).append("\n");
            sb.append("  小爱: ").append(d.get("agentResponse")).append("\n");
        }
        return sb.toString();
    }

    private boolean isConversationalIntent(String intent) {
        if (intent == null) return false;
        return intent.equals("GREETING") || intent.equals("LOVE_EXPRESSION")
            || intent.equals("HELP") || intent.equals("CASUAL_CHAT")
            || intent.equals("UNKNOWN") || intent.equals("SAVE_MEMORY")
            || intent.equals("UPDATE_PROFILE") || intent.equals("QUERY_CONTEXT");
    }

    /**
     * 清除用户的所有上下文记忆
     */
    public void clearContext(Long userId) {
        if (userId == null) {
            return;
        }

        redisTemplate.delete(getContextKey(userId));
        redisTemplate.delete(getDialogueKey(userId));
        log.info("已清除用户上下文记忆：userId={}", userId);
    }

    /**
     * 保存临时对话参数（如"明天"指的是哪天）
     */
    public void saveTempParam(Long userId, String key, String value, long expireMinutes) {
        if (userId == null || key == null) {
            return;
        }
        
        String tempKey = CONTEXT_PREFIX + userId + ":temp:" + key;
        redisTemplate.opsForValue().set(tempKey, value, expireMinutes, TimeUnit.MINUTES);
        log.info("保存临时参数：userId={}, key={}, value={}, expire={}min", userId, key, value, expireMinutes);
    }

    /**
     * 获取临时对话参数
     */
    public String getTempParam(Long userId, String key) {
        if (userId == null || key == null) {
            return null;
        }
        
        String tempKey = CONTEXT_PREFIX + userId + ":temp:" + key;
        return redisTemplate.opsForValue().get(tempKey);
    }
}
