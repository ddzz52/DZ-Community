package com.dz.couple.module.agent.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Agent 核心记忆（长期记忆）
 * 存储情侣基本信息、固定偏好等，永久保存
 */
@Data
public class AgentCoreMemory {
    private Long id;
    private Long coupleId;

    // 记忆类型：BASIC_INFO(基本信息), PREFERENCE(偏好), CUSTOM(自定义)
    private String memoryType;

    // 记忆键：如 love_date, birthday_him, birthday_her, dislike_food 等
    private String memoryKey;

    // 记忆值：如 "2024-01-01", "不喜欢辣", "喜欢旅行" 等
    private String memoryValue;

    // 重要程度 1-5，5最高
    private Integer importance;

    // 记忆来源：USER_INPUT(用户输入), SYSTEM_SYNC(系统同步), AI_EXTRACT(AI提取)
    private String source;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
