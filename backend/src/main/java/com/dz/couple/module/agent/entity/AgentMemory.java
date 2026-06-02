package com.dz.couple.module.agent.entity;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AgentMemory {
    private Long id;
    private Long coupleId;
    private String memoryType;
    private String memoryKey;
    private String memoryValue;
    private Integer importance;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
