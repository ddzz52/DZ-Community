package com.dz.couple.module.agent.service.impl;

import com.dz.couple.module.agent.dto.AgentChatRequest;
import com.dz.couple.module.agent.dto.AgentChatResponse;
import com.dz.couple.module.agent.entity.AgentIntentLog;
import com.dz.couple.module.agent.entity.AgentInteraction;
import com.dz.couple.module.agent.intent.IntentRecognitionResult;
import com.dz.couple.module.agent.intent.IntentRecognitionService;
import com.dz.couple.module.agent.intent.IntentDispatchService;
import com.dz.couple.module.agent.mapper.AgentIntentLogMapper;
import com.dz.couple.module.agent.mapper.AgentInteractionMapper;
import com.dz.couple.module.agent.memory.ContextMemoryService;
import com.dz.couple.module.agent.memory.MemoryManagerService;
import com.dz.couple.module.notification.NotificationTypes;
import com.dz.couple.module.notification.service.NotificationService;
import com.dz.couple.module.user.dto.UserVO;
import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.UserMapper;
import com.dz.couple.module.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

/**
 * AI 管家核心服务 — V2 重构版
 * 集成火山引擎 LLM 意图识别层，覆盖全部业务模块
 *
 * 处理流程:
 * 1. 同步基础记忆
 * 2. 安全检查（敏感信息 / 不安全内容）
 * 3. 意图识别（LLM → Redis缓存 → 规则兜底）
 * 4. 意图分发（策略模式路由到对应 Handler）
 * 5. 保存交互记录 & 上下文
 */
@Service
public class AgentServiceImpl implements com.dz.couple.module.agent.service.AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    // ============ 安全检测正则 ============
    private static final Pattern SENSITIVE_LONG_DIGITS = Pattern.compile("\\b\\d{13,19}\\b");

    // ============ 新注入：意图识别层 ============
    @Autowired
    private IntentRecognitionService intentRecognitionService;

    @Autowired
    private IntentDispatchService intentDispatchService;

    @Autowired
    private AgentIntentLogMapper intentLogMapper;

    // ============ 原有依赖 ============
    @Autowired
    private MemoryManagerService memoryManager;

    @Autowired
    private ContextMemoryService contextMemoryService;

    @Autowired
    private AgentInteractionMapper interactionMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationService notificationService;

    // ==================== 核心对话入口 ====================

    @Override
    public AgentChatResponse chat(Long userId, Long coupleId, AgentChatRequest request) {
        String message = request.getMessage().trim();
        log.info("Agent 收到消息：coupleId={}, userId={}, message={}", coupleId, userId, message);

        AgentChatResponse response = new AgentChatResponse();
        List<String> executedActions = new ArrayList<>();

        try {
            // 1. 同步基础记忆
            syncBasicMemory(userId, coupleId);

            // 2. 安全检查
            if (isSensitive(message)) {
                response.setIntent("REFUSE_SENSITIVE");
                response.setEmotion("中立");
                response.setReply("我可以帮你做纪念日、日记、备忘录、记账等情侣空间内的事情，"
                        + "但不能处理银行卡号、密码、验证码等敏感信息。你可以换个问题问我哦~");
                response.setExecutedActions(Collections.emptyList());
                response.setNeedFollowUp(false);
                response.setIntentConfidence(1.0);
                response.setRecognitionSource("RULE");
                saveInteraction(userId, coupleId, request, response);
                return response;
            }

            if (isUnsafeOrProhibited(message)) {
                response.setIntent("REFUSE_UNSAFE");
                response.setEmotion("中立");
                response.setReply("这个请求我没法帮助处理。"
                        + "我可以帮你查纪念日、写日记、记账、建备忘录，或者聊聊天～");
                response.setExecutedActions(Collections.emptyList());
                response.setNeedFollowUp(false);
                response.setIntentConfidence(1.0);
                response.setRecognitionSource("RULE");
                saveInteraction(userId, coupleId, request, response);
                return response;
            }

            // 3. 意图识别（LLM → 缓存 → 规则兜底）
            long intentStart = System.currentTimeMillis();
            IntentRecognitionResult intentResult = intentRecognitionService.recognize(userId, coupleId, message);
            long intentLatency = System.currentTimeMillis() - intentStart;

            log.info("意图识别完成: intent={}, source={}, confidence={}, latency={}ms",
                    intentResult.getIntent(), intentResult.getSource(),
                    intentResult.getConfidence(), intentLatency);

            // 3a. 记录意图识别日志
            logIntent(userId, coupleId, message, intentResult, intentLatency);

            // 3b. LLM 直接回复（不需要调用系统功能时）
            if (intentResult.hasDirectReply()) {
                response.setReply(intentResult.getDirectReply());
                response.setIntent(intentResult.getIntent());
                response.setEmotion(analyzeEmotion(message));
                response.setExecutedActions(Collections.emptyList());
                response.setNeedFollowUp(false);
                response.setIntentConfidence(intentResult.getConfidence());
                response.setRecognitionSource(intentResult.getSource().name());
                saveInteraction(userId, coupleId, request, response);
                saveContext(userId, message, response.getReply(), intentResult.getIntent(), response.getExecutedActions());
                return response;
            }

            // 4. 意图分发 — 路由到对应的 Handler 执行业务逻辑
            response = intentDispatchService.dispatch(intentResult, userId, coupleId, message);

            // 5. 补充元数据
            response.setEmotion(analyzeEmotion(message));
            response.setIntentConfidence(intentResult.getConfidence());
            response.setRecognitionSource(intentResult.getSource().name());

            if (response.getExecutedActions() == null) {
                response.setExecutedActions(new ArrayList<String>());
            }

            // 6. 保存交互记录
            saveInteraction(userId, coupleId, request, response);

            // 7. 保存上下文记忆到 Redis
            saveContext(userId, message, response.getReply(),
                    intentResult.getIntent(), response.getExecutedActions());

        } catch (Exception e) {
            log.error("Agent 处理异常", e);
            response.setReply("抱歉，我遇到了一点小问题，请稍后再试~ 🥺");
            response.setEmotion("中立");
            response.setIntent("ERROR");
        }

        return response;
    }

    // ==================== 主动关怀（保持不变） ====================

    @Override
    public void proactiveCare(Long coupleId) {
        log.info("执行主动关怀：coupleId={}", coupleId);

        List<User> users = userMapper.listByCoupleId(coupleId);
        if (users == null || users.isEmpty()) {
            return;
        }

        for (User u : users) {
            if (u == null || u.getId() == null) {
                continue;
            }
            notificationService.ensureAnniversaryReminders(u.getId(), coupleId);
        }

        if (isLoveWordsTime()) {
            sendDailyLoveWords(coupleId, users);
        }

        if (isMoodCareTime()) {
            sendMoodCare(coupleId, users);
        }
    }

    // ==================== 安全检测 ====================

    private boolean isSensitive(String message) {
        if (message == null) return false;
        if (SENSITIVE_LONG_DIGITS.matcher(message).find()) return true;
        String m = message.toLowerCase();
        return m.contains("密码") || m.contains("银行卡") || m.contains("卡号")
                || m.contains("cvv") || m.contains("验证码") || m.contains("token");
    }

    private boolean isUnsafeOrProhibited(String message) {
        if (message == null) return false;
        String m = message.toLowerCase();
        return m.contains("自杀") || m.contains("杀人") || m.contains("爆炸")
                || m.contains("毒品") || m.contains("制毒") || m.contains("诈骗") || m.contains("黑客");
    }

    // ==================== 情感分析 ====================

    private String analyzeEmotion(String message) {
        if (message == null) return "中立";
        String m = message.toLowerCase();
        if (m.contains("开心") || m.contains("高兴") || m.contains("喜欢")
                || m.contains("爱你") || m.contains("幸福")) {
            return "开心";
        }
        if (m.contains("难过") || m.contains("伤心") || m.contains("委屈") || m.contains("失落")) {
            return "难过";
        }
        if (m.contains("生气") || m.contains("烦") || m.contains("讨厌") || m.contains("崩溃")) {
            return "烦躁";
        }
        return "中立";
    }

    // ==================== 记忆同步 ====================

    private void syncBasicMemory(Long userId, Long coupleId) {
        if (userId == null || coupleId == null) return;
        UserVO me = userService.getById(userId);
        if (me != null && me.getLoveDate() != null) {
            LocalDate loveDate = toLocalDate(me.getLoveDate());
            if (loveDate != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(loveDate, LocalDate.now());
                memoryManager.saveCoreMemory(coupleId, "BASIC_INFO", "love_date",
                        loveDate.toString(), 3, "SYSTEM_SYNC");
                memoryManager.saveCoreMemory(coupleId, "BASIC_INFO", "together_days",
                        String.valueOf(days) + "天", 3, "SYSTEM_SYNC");
            }
        }
        List<User> users = userMapper.listByCoupleId(coupleId);
        if (users != null && !users.isEmpty()) {
            List<String> nicks = new ArrayList<>();
            for (User u : users) {
                if (u != null && u.getNickname() != null && !u.getNickname().trim().isEmpty()) {
                    nicks.add(u.getNickname().trim());
                }
            }
            if (!nicks.isEmpty()) {
                memoryManager.saveCoreMemory(coupleId, "BASIC_INFO", "nicknames",
                        String.join("、", nicks), 2, "SYSTEM_SYNC");
            }
            // 同步个人记忆：星座
            for (User u : users) {
                if (u == null) continue;
                String prefix = (u.getNickname() != null && !u.getNickname().isEmpty())
                        ? u.getNickname() : "用户" + u.getId();
                if (u.getZodiac() != null && !u.getZodiac().isEmpty()) {
                    memoryManager.saveCoreMemory(coupleId, "PREFERENCE",
                            prefix + "_星座", u.getZodiac(), 1, "SYSTEM_SYNC");
                }
            }
        }
    }

    // ==================== 交互记录 ====================

    private void saveInteraction(Long userId, Long coupleId,
                                  AgentChatRequest request, AgentChatResponse response) {
        AgentInteraction interaction = new AgentInteraction();
        interaction.setCoupleId(coupleId);
        interaction.setUserId(userId);
        interaction.setInteractionType(request.getMessageType());
        interaction.setIntent(response.getIntent());
        interaction.setUserMessage(request.getMessage());
        interaction.setAgentResponse(response.getReply());
        interaction.setToolsUsed(actionsToJson(response.getExecutedActions()));
        interaction.setEmotionScore(emotionToScore(response.getEmotion()));
        interactionMapper.insert(interaction);
        // 将 DB 自增 ID 回传给前端，用于后续删除操作
        response.setInteractionId(interaction.getId());
    }

    // ==================== 意图日志 ====================

    private void logIntent(Long userId, Long coupleId, String message,
                           IntentRecognitionResult result, long latencyMs) {
        try {
            AgentIntentLog logEntry = new AgentIntentLog();
            logEntry.setCoupleId(coupleId);
            logEntry.setUserId(userId);
            logEntry.setMessage(message.length() > 500 ? message.substring(0, 500) : message);
            logEntry.setRecognizedIntent(result.getIntent());
            logEntry.setParamsJson(result.getParams() != null && !result.getParams().isEmpty()
                    ? toJsonQuietly(result.getParams()) : null);
            logEntry.setConfidence(result.getConfidence());
            logEntry.setSource(result.getSource().name());
            logEntry.setLatencyMs(latencyMs);
            intentLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("保存意图日志失败: {}", e.getMessage());
        }
    }

    // ==================== 上下文保存 ====================

    private void saveContext(Long userId, String userMessage, String agentReply,
                             String intent, List<String> actions) {
        try {
            contextMemoryService.saveContext(userId, userMessage, agentReply, intent, actions);
        } catch (Exception e) {
            log.warn("保存上下文记忆失败（可能Redis未启动）：userId={}", userId, e);
        }
    }

    // ==================== 主动关怀辅助方法 ====================

    private boolean isLoveWordsTime() {
        int hour = LocalDateTime.now().getHour();
        return hour >= 8 && hour <= 9;
    }

    private boolean isMoodCareTime() {
        int hour = LocalDateTime.now().getHour();
        return hour >= 20 && hour <= 21;
    }

    private void sendDailyLoveWords(Long coupleId, List<User> users) {
        String[] words = {
                "早安呀～今天也要一起好好爱对方。",
                "新的一天开始啦：把温柔留给最重要的人。",
                "今天也要记得说一句「我爱你」。"
        };
        String content = words[new Random().nextInt(words.length)];
        Date refDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        for (User u : users) {
            if (u == null || u.getId() == null) continue;
            notificationService.create(u.getId(), coupleId, NotificationTypes.SYSTEM,
                    "每日情话", content, 1L, refDate, true);
        }
    }

    private void sendMoodCare(Long coupleId, List<User> users) {
        String[] words = {
                "今天过得怎么样？要不要记录一篇日记，或者和我聊聊～",
                "给自己一个拥抱：今天最开心/最难的事是什么？",
                "要不要写个小备忘录，把明天想做的事安排一下？"
        };
        String content = words[new Random().nextInt(words.length)];
        Date refDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        for (User u : users) {
            if (u == null || u.getId() == null) continue;
            notificationService.create(u.getId(), coupleId, NotificationTypes.SYSTEM,
                    "心情关怀", content, 2L, refDate, true);
        }
    }

    // ==================== 工具方法 ====================

    private double emotionToScore(String emotion) {
        if ("开心".equals(emotion)) return 0.9;
        if ("难过".equals(emotion)) return 0.3;
        if ("烦躁".equals(emotion)) return 0.4;
        return 0.6;
    }

    private String actionsToJson(List<String> actions) {
        if (actions == null || actions.isEmpty()) return null;
        try {
            return JSON.writeValueAsString(actions);
        } catch (Exception e) {
            return null;
        }
    }

    private String toJsonQuietly(Object obj) {
        try {
            return JSON.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate toLocalDate(Date d) {
        if (d == null) return null;
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
