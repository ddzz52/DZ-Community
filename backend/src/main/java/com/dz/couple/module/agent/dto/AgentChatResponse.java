package com.dz.couple.module.agent.dto;
import lombok.Data;
import java.util.List;

@Data
public class AgentChatResponse {
    private String reply;
    private String intent;
    private List<String> executedActions;
    private String emotion;
    private Boolean needFollowUp;
    private String followUpSuggestion;

    /** 意图置信度 0.0~1.0 */
    private Double intentConfidence;

    /** 识别来源: LLM / RULE / CACHE */
    private String recognitionSource;

    /** 交互记录主键 ID（用于前端删除操作） */
    private Long interactionId;
}
