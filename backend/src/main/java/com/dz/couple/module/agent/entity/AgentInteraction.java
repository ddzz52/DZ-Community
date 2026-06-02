package com.dz.couple.module.agent.entity;

import lombok.Data;
import java.time.LocalDateTime;


@Data
public class AgentInteraction {
    private Long id;
    private Long coupleId;
    private Long userId;
    private String interactionType;
    private String intent;
    private String userMessage;
    private String agentResponse;
    private String toolsUsed;
    private Double emotionScore;
    private LocalDateTime createdAt;
}

