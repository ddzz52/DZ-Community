package com.dz.couple.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 火山引擎 Ark API 配置属性（纯属性绑定，与 Bean 定义分离）
 *
 * 豆包 API 调用需要两个核心参数：
 * 1. API Key（sk- 开头）— 从控制台「API Key 管理」获取
 * 2. Endpoint ID（ep- 开头）— 从控制台「推理接入点」获取，作为 model 传入请求体
 *
 * 文档: https://www.volcengine.com/docs/82379
 */
@ConfigurationProperties(prefix = "volcengine.ark")
public class VolcengineProperties {

    /** API Key，从火山引擎控制台「API Key 管理」获取，格式: sk-xxxxxxxx */
    private String apiKey;

    /** 推理接入点 ID，从控制台「推理接入点」获取，格式: ep-xxxxxxxx，将作为请求体中 model 字段的值 */
    private String endpointId;

    /** API 地址 */
    private String baseUrl = "https://ark.cn-beijing.volces.com/api/v3";

    /** 模型参数: doubao-pro-32k / doubao-lite-32k（用于日志记录等辅助用途，实际请求使用 endpointId） */
    private String model = "doubao-pro-32k";

    /** 超时时间(毫秒) */
    private int timeout = 30000;

    /** 最大重试次数 */
    private int maxRetries = 2;

    /** 是否启用 LLM 意图识别 */
    private boolean enabled = true;

    /** 温度参数 0.0-1.0，越高回复越自然多变 */
    private double temperature = 0.85;

    /** 最大输出 token 数 */
    private int maxTokens = 2048;

    // ========== getters / setters ==========

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getEndpointId() { return endpointId; }
    public void setEndpointId(String endpointId) { this.endpointId = endpointId; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    // ========== 便捷方法 ==========

    /**
     * 返回实际用于请求体 model 字段的值：优先使用 endpointId，未配置时回退到 model
     */
    public String effectiveModel() {
        return (endpointId != null && !endpointId.trim().isEmpty()) ? endpointId.trim() : model;
    }

    /**
     * 检查 API Key 和 Endpoint ID 是否均已配置
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty()
                && endpointId != null && !endpointId.trim().isEmpty();
    }
}
