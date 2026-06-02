package com.dz.couple.module.agent.intent;

import java.util.Collections;
import java.util.Map;

/**
 * 意图识别结果
 */
public class IntentRecognitionResult {

    /** 识别出的意图编码 */
    private String intent;

    /** LLM提取的参数，可能为空Map */
    private Map<String, Object> params;

    /** 置信度 0.0~1.0 */
    private double confidence;

    /** LLM直接回复内容(当不需要调用功能时)，可为null */
    private String directReply;

    /** 识别来源 */
    private RecognitionSource source;

    /** 识别耗时(毫秒) */
    private long latencyMs;

    public IntentRecognitionResult() {
        this.params = Collections.emptyMap();
    }

    public enum RecognitionSource {
        /** 火山引擎大模型识别 */
        LLM,
        /** 本地规则正则匹配 */
        RULE,
        /** Redis 缓存命中 */
        CACHE
    }

    // ========== Builder 风格 ==========

    public static IntentRecognitionResult of(String intent, RecognitionSource source) {
        IntentRecognitionResult r = new IntentRecognitionResult();
        r.intent = intent;
        r.source = source;
        r.confidence = source == RecognitionSource.RULE ? 0.8 : 0.95;
        return r;
    }

    public static IntentRecognitionResult llm(String intent, Map<String, Object> params, double confidence) {
        IntentRecognitionResult r = new IntentRecognitionResult();
        r.intent = intent;
        r.params = params != null ? params : Collections.<String, Object>emptyMap();
        r.confidence = confidence;
        r.source = RecognitionSource.LLM;
        return r;
    }

    public static IntentRecognitionResult rule(String intent, Map<String, Object> params) {
        IntentRecognitionResult r = new IntentRecognitionResult();
        r.intent = intent;
        r.params = params != null ? params : Collections.<String, Object>emptyMap();
        r.confidence = 0.8;
        r.source = RecognitionSource.RULE;
        return r;
    }

    public static IntentRecognitionResult cache(String intent, Map<String, Object> params, double confidence) {
        IntentRecognitionResult r = new IntentRecognitionResult();
        r.intent = intent;
        r.params = params != null ? params : Collections.<String, Object>emptyMap();
        r.confidence = confidence;
        r.source = RecognitionSource.CACHE;
        return r;
    }

    public static IntentRecognitionResult directReply(String reply) {
        IntentRecognitionResult r = new IntentRecognitionResult();
        r.intent = IntentEnum.CASUAL_CHAT.getCode();
        r.directReply = reply;
        r.confidence = 0.9;
        r.source = RecognitionSource.LLM;
        return r;
    }

    // ========== 便捷方法 ==========

    public boolean isUnknown() {
        return IntentEnum.UNKNOWN.getCode().equals(intent) || intent == null;
    }

    public boolean hasDirectReply() {
        return directReply != null && !directReply.isEmpty();
    }

    // ========== Getters / Setters ==========

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getDirectReply() { return directReply; }
    public void setDirectReply(String directReply) { this.directReply = directReply; }

    public RecognitionSource getSource() { return source; }
    public void setSource(RecognitionSource source) { this.source = source; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
}
