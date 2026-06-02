package com.dz.couple.module.agent.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Agent 场景记忆（中期记忆）
 * 存储近期对话、操作记录、临时偏好等，有效期30天
 */
@Data
public class AgentSceneMemory {
    private Long id;
    private Long coupleId;
    private Long userId;

    // 记忆类型：DIALOGUE(对话记录), ACTION(操作记录), TEMP_PREFERENCE(临时偏好)
    private String memoryType;

    // 记忆内容摘要
    private String content;

    // 原始数据（JSON格式）
    private String rawData;

    // 关联的意图或操作类型
    private String relatedIntent;

    // 重要程度 1-5
    private Integer importance;

    // 过期时间
    private LocalDateTime expireAt;

    private LocalDateTime createdAt;
}
