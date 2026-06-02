package com.dz.couple.module.agent.volcengine;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * 火山引擎 Ark Chat Completion 请求 DTO（OpenAI 兼容格式）
 * 注意: 火山引擎 API 使用 snake_case 命名，必须显式标注 @JsonProperty
 */
public class VolcengineChatRequest {
    private String model;
    private List<Message> messages;
    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private List<Tool> tools;

    @JsonProperty("tool_choice")
    private String toolChoice;

    public static class Message {
        private String role;
        private String content;

        public Message() {}
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class Tool {
        private String type = "function";
        private Function function;

        public Tool() {}
        public Tool(Function function) {
            this.function = function;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Function getFunction() { return function; }
        public void setFunction(Function function) { this.function = function; }
    }

    public static class Function {
        private String name;
        private String description;
        private Map<String, Object> parameters;

        public Function() {}
        public Function(String name, String description, Map<String, Object> parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }

    // ========== getters/setters ==========

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public List<Tool> getTools() { return tools; }
    public void setTools(List<Tool> tools) { this.tools = tools; }
    public String getToolChoice() { return toolChoice; }
    public void setToolChoice(String toolChoice) { this.toolChoice = toolChoice; }
}
