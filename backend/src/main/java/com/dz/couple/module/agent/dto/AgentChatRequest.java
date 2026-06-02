package com.dz.couple.module.agent.dto;
import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class AgentChatRequest {
    @NotBlank(message = "消息不能为空")
    private String message;

    private String messageType = "CHAT";
}
