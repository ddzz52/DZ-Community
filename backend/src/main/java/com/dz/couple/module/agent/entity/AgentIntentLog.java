package com.dz.couple.module.agent.entity;

import java.util.Date;

/**
 * 意图识别日志实体 — 用于监控、分析和调优意图识别准确率
 */
public class AgentIntentLog {

    private Long id;
    private Long coupleId;
    private Long userId;
    private String message;
    private String recognizedIntent;
    private String paramsJson;
    private Double confidence;
    private String source;       // LLM / RULE / CACHE
    private Long latencyMs;
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCoupleId() { return coupleId; }
    public void setCoupleId(Long coupleId) { this.coupleId = coupleId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getRecognizedIntent() { return recognizedIntent; }
    public void setRecognizedIntent(String recognizedIntent) { this.recognizedIntent = recognizedIntent; }

    public String getParamsJson() { return paramsJson; }
    public void setParamsJson(String paramsJson) { this.paramsJson = paramsJson; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
