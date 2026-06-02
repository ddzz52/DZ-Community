package com.dz.couple.module.agent.intent.handler;

import com.dz.couple.module.agent.dto.AgentChatResponse;
import com.dz.couple.module.agent.intent.IntentEnum;

import java.util.Map;
import java.util.Set;

/**
 * 意图处理器接口
 * 每个实现类负责处理一组相关的意图，执行实际的业务逻辑
 *
 * 实现类标注 @Component，Spring 自动发现并注册到 IntentDispatchService
 */
public interface IntentHandler {

    /**
     * 返回该处理器支持的意图集合
     */
    Set<IntentEnum> supportedIntents();

    /**
     * 处理意图
     *
     * @param userId   用户ID
     * @param coupleId 情侣ID
     * @param message  原始用户消息
     * @param params   LLM 或规则提取的参数 (可能为空)
     * @return Agent 回复
     */
    AgentChatResponse handle(Long userId, Long coupleId, String message, Map<String, Object> params);
}
