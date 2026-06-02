package com.dz.couple.module.agent.volcengine;

import com.dz.couple.config.VolcengineProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 火山引擎 Ark API 客户端
 * 封装 Chat Completion 调用，含熔断、重试、超时控制
 */
@Component
public class VolcengineClient {

    private static final Logger log = LoggerFactory.getLogger(VolcengineClient.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final VolcengineProperties properties;

    // ============ 简易熔断器 ============
    private volatile int consecutiveFailures = 0;
    private volatile long circuitOpenUntil = 0L;
    private static final int CIRCUIT_BREAKER_THRESHOLD = 3;
    private static final long CIRCUIT_COOLDOWN_MS = 60_000L; // 熔断 60 秒

    public VolcengineClient(@Qualifier("volcengineRestTemplate") RestTemplate restTemplate,
                            VolcengineProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * 调用火山引擎 Chat Completion，返回解析后的 FunctionCall 结果
     *
     * @param systemPrompt 系统提示词（含所有功能定义）
     * @param userMessage  用户输入
     * @param tools        可用工具列表
     * @return 解析结果 {name, arguments}，失败返回 null
     */
    public FunctionCallResult callWithFunctions(String systemPrompt, String userMessage,
                                                 List<VolcengineChatRequest.Tool> tools) {
        if (!properties.isEnabled()) {
            log.debug("火山引擎 LLM 未启用，跳过调用");
            return null;
        }

        // 校验必要配置：API Key + Endpoint ID 缺一不可
        if (!properties.isConfigured()) {
            log.error("火山引擎配置不完整: apiKey={}, endpointId={}. "
                    + "请在配置中设置 volcengine.ark.api-key 和 volcengine.ark.endpoint-id",
                    properties.getApiKey() != null && !properties.getApiKey().trim().isEmpty() ? "已配置" : "未配置",
                    properties.getEndpointId() != null && !properties.getEndpointId().trim().isEmpty() ? "已配置" : "未配置");
            return null;
        }

        if (isCircuitOpen()) {
            log.warn("熔断器开启，跳过 LLM 调用 (连续失败={})", consecutiveFailures);
            return null;
        }

        long start = System.currentTimeMillis();

        for (int attempt = 0; attempt <= properties.getMaxRetries(); attempt++) {
            try {
                VolcengineChatRequest req = buildRequest(systemPrompt, userMessage, tools);
                VolcengineChatResponse resp = doHttpCall(req);
                FunctionCallResult result = parseResponse(resp);

                long elapsed = System.currentTimeMillis() - start;
                log.info("火山引擎 LLM 调用成功: intent={}, latency={}ms, tokens={}",
                        result != null ? result.getName() : "null",
                        elapsed,
                        resp.getUsage() != null ? resp.getUsage().getTotalTokens() : 0);

                onSuccess();
                return result;

            } catch (RestClientException e) {
                long elapsed = System.currentTimeMillis() - start;
                log.warn("火山引擎 API 调用失败 (attempt={}/{}): latency={}ms, error={}",
                        attempt + 1, properties.getMaxRetries() + 1, elapsed, e.getMessage());

                if (attempt >= properties.getMaxRetries()) {
                    onFailure();
                    return null;
                }

                // 退避重试
                try {
                    Thread.sleep((attempt + 1) * 500L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return null;
                }

            } catch (Exception e) {
                log.error("火山引擎 LLM 调用异常", e);
                onFailure();
                return null;
            }
        }

        return null;
    }

    // ============ 私有方法 ============

    private VolcengineChatRequest buildRequest(String systemPrompt, String userMessage,
                                                List<VolcengineChatRequest.Tool> tools) {
        VolcengineChatRequest req = new VolcengineChatRequest();
        // 关键：model 字段必须填 Endpoint ID（ep-xxx），不能填 doubao-pro-32k 这类通用名
        req.setModel(properties.effectiveModel());
        req.setMessages(Collections.unmodifiableList(java.util.Arrays.asList(
                new VolcengineChatRequest.Message("system", systemPrompt),
                new VolcengineChatRequest.Message("user", userMessage)
        )));
        req.setTemperature(properties.getTemperature());
        req.setMaxTokens(properties.getMaxTokens());
        req.setTools(tools);
        req.setToolChoice("auto");
        return req;
    }

    private VolcengineChatResponse doHttpCall(VolcengineChatRequest req) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + properties.getApiKey());

        String url = properties.getBaseUrl() + "/chat/completions";

        // 详细日志：确认请求参数正确（仅 DEBUG 级别，避免泄露 Key）
        log.debug("火山引擎 API 请求: url={}, model={}, messages={}, tools={}",
                url, req.getModel(),
                req.getMessages() != null ? req.getMessages().size() : 0,
                req.getTools() != null ? req.getTools().size() : 0);

        HttpEntity<VolcengineChatRequest> entity = new HttpEntity<>(req, headers);

        try {
            ResponseEntity<VolcengineChatResponse> resp = restTemplate.exchange(
                    url, HttpMethod.POST, entity, VolcengineChatResponse.class);

            if (resp.getBody() == null) {
                throw new RestClientException("火山引擎返回空body, status=" + resp.getStatusCodeValue());
            }

            // 校验返回是否有 usage（有 usage 才是真正计费的成功调用）
            if (resp.getBody().getUsage() == null) {
                log.warn("火山引擎返回无 usage 字段，可能是无效调用（不计次）。"
                        + "请检查: 1) API Key 格式是否为 sk-xxx "
                        + "2) model 字段是否为有效的 Endpoint ID (ep-xxx) "
                        + "3) Endpoint ID 是否已部署上线");
            }
            return resp.getBody();

        } catch (RestClientException e) {
            // 读取错误响应体，便于排查问题
            try {
                ResponseEntity<String> errResp = restTemplate.exchange(
                        url, HttpMethod.POST, entity, String.class);
                String errBody = errResp.getBody();
                log.error("火山引擎 API 错误: status={}, url={}, model={}, body={}",
                        errResp.getStatusCodeValue(),
                        url,
                        req.getModel(),
                        errBody != null ? errBody.substring(0, Math.min(1000, errBody.length())) : "null");

                // 常见错误码提示
                if (errResp.getStatusCodeValue() == 401) {
                    log.error(">>> 401 鉴权失败！请检查: "
                            + "1) Authorization 格式是否为 'Bearer sk-xxx'（注意 Bearer 后必须有空格）"
                            + "2) API Key 是否有效、是否已过期");
                } else if (errResp.getStatusCodeValue() == 404 || errResp.getStatusCodeValue() == 400) {
                    log.error(">>> {} 错误！请检查 model/endpointId 字段: "
                            + "当前使用 model='{}'，必须是有效的推理接入点 ID（ep-xxx）或端点名称",
                            errResp.getStatusCodeValue(), req.getModel());
                }
            } catch (Exception ignored) {
            }
            throw e;
        }
    }

    private FunctionCallResult parseResponse(VolcengineChatResponse resp) {
        // 优先处理 tool_calls（function calling）
        VolcengineChatResponse.ToolCall toolCall = resp.firstToolCall();
        if (toolCall != null && toolCall.getFunction() != null) {
            FunctionCallResult result = new FunctionCallResult();
            result.setName(toolCall.getFunction().getName());
            result.setArguments(toolCall.getFunction().getArguments());
            log.info("LLM function_call 解析成功: name={}, args={}",
                    result.getName(), result.getArguments());
            return result;
        }

        // 没有 tool_calls，检查是否有 content 文本（直接回复）
        String text = resp.contentText();
        if (text != null && !text.trim().isEmpty()) {
            log.info("LLM 直接回复(无function_call): text={}", text.substring(0, Math.min(100, text.length())));
            FunctionCallResult result = new FunctionCallResult();
            result.setDirectReply(text.trim());
            return result;
        }

        // 诊断日志：记录 API 返回了什么
        if (resp.firstChoice() != null && resp.firstChoice().getMessage() != null) {
            log.warn("LLM 响应解析失败: choice存在但无tool_calls也无content. "
                    + "finishReason={}, role={}, hasContent={}, hasToolCalls={}",
                    resp.firstChoice().getFinishReason(),
                    resp.firstChoice().getMessage().getRole(),
                    resp.firstChoice().getMessage().getContent() != null,
                    resp.firstChoice().getMessage().getToolCalls() != null);
        } else {
            log.warn("LLM 响应解析失败: choices为空或message为null. choicesCount={}",
                    resp.getChoices() != null ? resp.getChoices().size() : 0);
        }
        return null;
    }

    // ============ 熔断器 ============

    private boolean isCircuitOpen() {
        if (consecutiveFailures < CIRCUIT_BREAKER_THRESHOLD) {
            return false;
        }
        if (System.currentTimeMillis() > circuitOpenUntil) {
            // 冷却期过，半开状态
            consecutiveFailures = 0;
            circuitOpenUntil = 0L;
            log.info("熔断器半开，尝试恢复 LLM 调用");
            return false;
        }
        return true;
    }

    private void onSuccess() {
        consecutiveFailures = 0;
        circuitOpenUntil = 0L;
    }

    private void onFailure() {
        consecutiveFailures++;
        if (consecutiveFailures >= CIRCUIT_BREAKER_THRESHOLD) {
            circuitOpenUntil = System.currentTimeMillis() + CIRCUIT_COOLDOWN_MS;
            log.error("熔断器触发! 连续失败 {} 次，冷却 {} 秒",
                    consecutiveFailures, CIRCUIT_COOLDOWN_MS / 1000);
        }
    }

    // ============ 内部类 ============

    public static class FunctionCallResult {
        private String name;        // function name (intent code)
        private String arguments;   // JSON string of parameters
        private String directReply; // direct text reply (no function call)

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getArguments() { return arguments; }
        public void setArguments(String arguments) { this.arguments = arguments; }

        public String getDirectReply() { return directReply; }
        public void setDirectReply(String directReply) { this.directReply = directReply; }

        public boolean isFunctionCall() {
            return name != null && !name.isEmpty();
        }

        public boolean isDirectReply() {
            return directReply != null && !directReply.isEmpty();
        }
    }
}
