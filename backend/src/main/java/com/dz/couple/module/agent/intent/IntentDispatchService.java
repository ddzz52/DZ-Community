package com.dz.couple.module.agent.intent;

import com.dz.couple.module.agent.dto.AgentChatResponse;
import com.dz.couple.module.agent.intent.handler.IntentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 意图分发服务 — 将识别出的意图路由到对应的 Handler 执行
 *
 * 使用策略模式：Spring 自动注入所有 IntentHandler Bean，
 * 构建 IntentEnum → IntentHandler 的映射表
 */
@Service
public class IntentDispatchService {

    private static final Logger log = LoggerFactory.getLogger(IntentDispatchService.class);

    @Autowired(required = false)
    private List<IntentHandler> handlers;

    private final Map<IntentEnum, IntentHandler> handlerMap = new EnumMap<>(IntentEnum.class);

    @PostConstruct
    public void init() {
        if (handlers == null || handlers.isEmpty()) {
            log.warn("未发现任何 IntentHandler Bean，AI 将无法执行具体功能");
            return;
        }
        for (IntentHandler handler : handlers) {
            Set<IntentEnum> supported = handler.supportedIntents();
            if (supported != null) {
                for (IntentEnum intent : supported) {
                    handlerMap.put(intent, handler);
                }
            }
        }
        log.info("IntentDispatchService 初始化完成，已注册 {} 个意图处理器, 覆盖 {} 个意图",
                handlers.size(), handlerMap.size());
    }

    /**
     * 分发意图到对应的 Handler 并执行
     *
     * @param result   意图识别结果
     * @param userId   用户ID
     * @param coupleId 情侣ID
     * @param message  原始消息
     * @return Agent 回复
     */
    public AgentChatResponse dispatch(IntentRecognitionResult result,
                                       Long userId, Long coupleId, String message) {
        IntentEnum intentEnum = IntentEnum.fromCode(result.getIntent());
        IntentHandler handler = handlerMap.get(intentEnum);

        if (handler != null) {
            // 创建新的可变 HashMap，避免原 params 为不可变集合时 put 抛异常
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> originalParams = result.getParams();
            if (originalParams != null) {
                params.putAll(originalParams);
            }
            params.put("_intent", intentEnum.getCode());

            log.info("意图分发: intent={}, handler={}, params={}", result.getIntent(),
                    handler.getClass().getSimpleName(), params);
            return handler.handle(userId, coupleId, message, params);
        }

        // 无对应 Handler，返回默认回复
        log.warn("未找到意图处理器: intent={}", result.getIntent());
        AgentChatResponse fallback = new AgentChatResponse();
        fallback.setIntent(result.getIntent());
        fallback.setEmotion("中立");
        fallback.setReply("这个功能我还在学习中~ 你可以试试让我帮你查纪念日、写日记、记账、看照片等 💕");
        fallback.setExecutedActions(Collections.<String>emptyList());
        fallback.setNeedFollowUp(false);
        return fallback;
    }
}
