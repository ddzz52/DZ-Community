package com.dz.couple.module.agent.service;

import com.dz.couple.module.agent.dto.AgentChatRequest;
import com.dz.couple.module.agent.dto.AgentChatResponse;

public interface AgentService {
    AgentChatResponse chat(Long userId, Long coupleId, AgentChatRequest request);
    void proactiveCare(Long coupleId);
}
