package com.dz.couple.module.agent.volcengine;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 火山引擎 Ark Chat Completion 响应 DTO（OpenAI 兼容格式）
 * 注意: 火山引擎 API 返回 snake_case 字段，必须显式标注 @JsonProperty
 */
public class VolcengineChatResponse {

    private String id;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    public static class Choice {
        private Integer index;
        private Message message;

        @JsonProperty("finish_reason")
        private String finishReason;

        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }
        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    }

    public static class Message {
        private String role;
        private String content;

        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public List<ToolCall> getToolCalls() { return toolCalls; }
        public void setToolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; }
    }

    public static class ToolCall {
        private String id;
        private String type;
        private FunctionCall function;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public FunctionCall getFunction() { return function; }
        public void setFunction(FunctionCall function) { this.function = function; }
    }

    public static class FunctionCall {
        private String name;
        private String arguments;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getArguments() { return arguments; }
        public void setArguments(String arguments) { this.arguments = arguments; }
    }

    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;

        public Integer getPromptTokens() { return promptTokens; }
        public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
        public Integer getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
        public Integer getTotalTokens() { return totalTokens; }
        public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    }

    // ========== getters/setters ==========

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }
    public Usage getUsage() { return usage; }
    public void setUsage(Usage usage) { this.usage = usage; }

    // ========== 便捷方法 ==========

    public Choice firstChoice() {
        return choices != null && !choices.isEmpty() ? choices.get(0) : null;
    }

    public ToolCall firstToolCall() {
        Choice c = firstChoice();
        if (c == null || c.getMessage() == null) return null;
        List<ToolCall> calls = c.getMessage().getToolCalls();
        return calls != null && !calls.isEmpty() ? calls.get(0) : null;
    }

    public String contentText() {
        Choice c = firstChoice();
        if (c == null || c.getMessage() == null) return null;
        return c.getMessage().getContent();
    }
}
