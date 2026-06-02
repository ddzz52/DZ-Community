package com.dz.couple.module.agent.intent;

import com.dz.couple.module.agent.volcengine.VolcengineChatRequest;
import com.dz.couple.module.agent.volcengine.VolcengineClient;
import com.dz.couple.module.agent.volcengine.VolcengineClient.FunctionCallResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 意图识别服务 — 混合识别引擎
 * 流程: Redis缓存 → 火山引擎LLM → 规则正则兜底
 * 各级超时/失败自动降级
 */
@Service
public class IntentRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(IntentRecognitionService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String INTENT_CACHE_PREFIX = "agent:intent:";
    private static final long CACHE_TTL_MINUTES = 5;

    @Autowired
    private VolcengineClient volcengineClient;

    @Autowired
    private IntentCatalogService catalogService;

    @Autowired
    private com.dz.couple.module.agent.memory.ContextMemoryService contextMemoryService;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /** 工具列表懒加载缓存 */
    private volatile List<VolcengineChatRequest.Tool> cachedTools;

    // ==================== 核心识别方法 ====================

    /**
     * 识别用户消息的意图
     *
     * 优化流程（降低成本）:
     * 1. Redis 缓存
     * 2. 本地规则预过滤（命中高频简单意图直接返回，省去 LLM 调用）
     * 3. 火山引擎 LLM（仅复杂意图走 LLM）
     * 4. 规则兜底
     */
    public IntentRecognitionResult recognize(Long userId, Long coupleId, String message) {
        if (message == null || message.trim().isEmpty()) {
            return IntentRecognitionResult.rule(IntentEnum.CASUAL_CHAT.getCode(), null);
        }

        String normalized = message.trim();

        // Layer 1: Redis 缓存
        IntentRecognitionResult cached = checkCache(userId, normalized);
        if (cached != null) {
            log.info("意图缓存命中: userId={}, intent={}", userId, cached.getIntent());
            return cached;
        }

        // Layer 2: 本地预过滤 — 高频简单意图直接匹配，省掉 LLM Token
        IntentRecognitionResult quickMatch = quickMatch(normalized);
        if (quickMatch != null) {
            log.info("本地预过滤命中: intent={}, 跳过 LLM 调用", quickMatch.getIntent());
            cacheResult(userId, normalized, quickMatch);
            return quickMatch;
        }

        // Layer 3: 火山引擎 LLM（复杂意图 + 自然对话）
        IntentRecognitionResult llmResult = recognizeByLlm(coupleId, userId, normalized);
        if (llmResult != null) {
            cacheResult(userId, normalized, llmResult);
            return llmResult;
        }

        // Layer 4: 规则正则兜底
        IntentRecognitionResult ruleResult = recognizeByRule(normalized);
        log.info("LLM 未命中，规则兜底: intent={}", ruleResult.getIntent());
        cacheResult(userId, normalized, ruleResult);
        return ruleResult;
    }

    /**
     * 本地快速预过滤 — 匹配高频简单意图
     * 这些意图占日常对话 60%+，本地匹配零成本
     */
    private IntentRecognitionResult quickMatch(String message) {
        String m = message.toLowerCase().trim();

        // 纯表情/符号消息：不含任何中文、英文、数字 → 直接当闲聊处理
        if (isPureEmojiOrSymbol(m)) {
            return IntentRecognitionResult.rule(IntentEnum.CASUAL_CHAT.getCode(), null);
        }

        // 短问候（≤5字）
        if (m.length() <= 5) {
            if (containsAny(m, "你好", "hi", "hello", "在吗", "嗨", "早", "晚安", "拜拜")) {
                return IntentRecognitionResult.rule(IntentEnum.GREETING.getCode(), null);
            }
            if (containsAny(m, "爱你", "想你", "么么", "抱抱", "喜欢")) {
                return IntentRecognitionResult.rule(IntentEnum.LOVE_EXPRESSION.getCode(), null);
            }
        }

        // 帮助类
        if (containsAny(m, "你能做什么", "有什么功能", "你能干嘛", "怎么用", "帮助")) {
            return IntentRecognitionResult.rule(IntentEnum.HELP.getCode(), null);
        }

        // 恋爱天数（高频）
        if (containsAny(m, "在一起", "恋爱", "相恋") && containsAny(m, "天", "多久", "多少", "第几天")) {
            return IntentRecognitionResult.rule(IntentEnum.QUERY_TOGETHER_DAYS.getCode(), null);
        }

        // 需要 LLM 处理的复杂意图 — 返回 null 让 LLM 处理
        return null;
    }

    // ==================== Layer 3: LLM 识别 + 自然对话 ====================

    private IntentRecognitionResult recognizeByLlm(Long coupleId, Long userId, String message) {
        long start = System.currentTimeMillis();

        try {
            // 构建对话上下文，让 LLM 回复更连贯
            String conversationHistory = contextMemoryService.buildContextText(userId);
            String systemPrompt = catalogService.buildSystemPrompt(coupleId, conversationHistory);
            List<VolcengineChatRequest.Tool> tools = getTools();

            FunctionCallResult callResult = volcengineClient.callWithFunctions(
                    systemPrompt, message, tools);

            long latency = System.currentTimeMillis() - start;

            if (callResult == null) {
                return null; // LLM 调用失败，降级到规则
            }

            // LLM 选择直接回复（不调用功能）
            if (callResult.isDirectReply()) {
                IntentRecognitionResult r = IntentRecognitionResult.directReply(callResult.getDirectReply());
                r.setLatencyMs(latency);
                return r;
            }

            // LLM 返回了 function call
            if (callResult.isFunctionCall()) {
                IntentEnum intent = IntentEnum.fromCode(callResult.getName());
                Map<String, Object> params = parseArguments(callResult.getArguments());
                IntentRecognitionResult r = IntentRecognitionResult.llm(
                        intent.getCode(), params, 0.9);
                r.setLatencyMs(latency);
                return r;
            }

            return null;

        } catch (Exception e) {
            log.warn("LLM 意图识别异常: {}", e.getMessage());
            return null;
        }
    }

    // ==================== Layer 3: 规则正则兜底 ====================

    /**
     * 规则正则快速匹配 — 保留原系统的全部正则模式，并扩展覆盖更多模块
     * 当火山引擎 LLM 不可用时作为兜底方案
     */
    private IntentRecognitionResult recognizeByRule(String message) {
        String m = message.toLowerCase();

        // 问候
        if (containsAny(m, "你好", "hi", "hello", "在吗", "嗨", "早啊", "晚上好")) {
            return IntentRecognitionResult.rule(IntentEnum.GREETING.getCode(), null);
        }

        // 爱意表达
        if (containsAny(m, "爱你", "想你", "喜欢你", "爱你哦", "么么哒", "抱抱")) {
            return IntentRecognitionResult.rule(IntentEnum.LOVE_EXPRESSION.getCode(), null);
        }

        // 询问帮助
        if (containsAny(m, "你能做什么", "有什么功能", "你能干嘛", "帮我", "help", "怎么用")) {
            return IntentRecognitionResult.rule(IntentEnum.HELP.getCode(), null);
        }

        // 在一起天数
        if (containsAny(m, "在一起", "恋爱", "相恋") && containsAny(m, "天", "多久", "多少")) {
            return IntentRecognitionResult.rule(IntentEnum.QUERY_TOGETHER_DAYS.getCode(), null);
        }

        // 纪念日相关
        if (containsAny(m, "纪念日", "周年", "生日", "节日", "还有.*天", "下次")) {
            if (containsAny(m, "添加", "新增", "创建", "记录")) {
                return IntentRecognitionResult.rule(IntentEnum.CREATE_ANNIVERSARY.getCode(), null);
            }
            return IntentRecognitionResult.rule(IntentEnum.QUERY_ANNIVERSARY.getCode(), null);
        }

        // 日记相关
        if (containsAny(m, "日记", "心情")) {
            if (containsAny(m, "写", "记", "记录", "添加")) {
                return IntentRecognitionResult.rule(IntentEnum.CREATE_DIARY.getCode(), null);
            }
            if (containsAny(m, "统计", "分析", "什么心情")) {
                return IntentRecognitionResult.rule(IntentEnum.DIARY_STATS.getCode(), null);
            }
            return IntentRecognitionResult.rule(IntentEnum.QUERY_DIARY.getCode(), null);
        }

        // 备忘录相关
        if (containsAny(m, "备忘录", "提醒我", "待办", "备忘", "记得")) {
            if (containsAny(m, "完成", "做完了", "搞定", "done")) {
                return IntentRecognitionResult.rule(IntentEnum.COMPLETE_MEMO.getCode(), null);
            }
            if (containsAny(m, "添加", "新增", "创建", "记一下", "提醒我", "记得")) {
                return IntentRecognitionResult.rule(IntentEnum.CREATE_MEMO.getCode(), null);
            }
            return IntentRecognitionResult.rule(IntentEnum.QUERY_MEMO.getCode(), null);
        }

        // 记账相关
        if (containsAny(m, "记账", "记账本", "花了", "消费", "账单", "开销", "支出", "收入", "报销")) {
            if (containsAny(m, "统计", "汇总", "分析", "多少", "看看", "查")) {
                if (containsAny(m, "月", "年", "分类", "占比")) {
                    return IntentRecognitionResult.rule(IntentEnum.ACCOUNT_STATS.getCode(), null);
                }
                return IntentRecognitionResult.rule(IntentEnum.QUERY_ACCOUNT.getCode(), null);
            }
            return IntentRecognitionResult.rule(IntentEnum.ADD_ACCOUNT.getCode(), null);
        }

        // 照片相册
        if (containsAny(m, "照片", "图片", "拍照", "相片")) {
            return IntentRecognitionResult.rule(IntentEnum.QUERY_PHOTO.getCode(), null);
        }
        if (containsAny(m, "相册", "相簿")) {
            return IntentRecognitionResult.rule(IntentEnum.QUERY_ALBUM.getCode(), null);
        }

        // 心愿 / 刮刮乐
        if (containsAny(m, "刮刮乐", "刮开", "刮卡")) {
            if (containsAny(m, "创建", "制作", "做", "添加")) {
                return IntentRecognitionResult.rule(IntentEnum.CREATE_WISH_SCRATCH.getCode(), null);
            }
            return IntentRecognitionResult.rule(IntentEnum.QUERY_WISH_SCRATCH.getCode(), null);
        }
        if (containsAny(m, "心愿", "愿望", "想做的事", "bucket")) {
            if (containsAny(m, "添加", "新增", "加", "记录")) {
                return IntentRecognitionResult.rule(IntentEnum.ADD_WISH.getCode(), null);
            }
            if (containsAny(m, "转盘", "抽", "随机", "roulette")) {
                return IntentRecognitionResult.rule(IntentEnum.SPIN_WISH.getCode(), null);
            }
            return IntentRecognitionResult.rule(IntentEnum.QUERY_WISH.getCode(), null);
        }

        // 消息
        if (containsAny(m, "消息", "未读", "新消息", "聊天")) {
            return IntentRecognitionResult.rule(IntentEnum.QUERY_MESSAGE.getCode(), null);
        }

        // 经期
        if (containsAny(m, "姨妈", "月经", "经期", "大姨妈", "例假", "period", "排卵", "易孕")) {
            if (containsAny(m, "记录", "设置", "更新", "修改", "来了")) {
                return IntentRecognitionResult.rule(IntentEnum.RECORD_PERIOD.getCode(), null);
            }
            return IntentRecognitionResult.rule(IntentEnum.QUERY_PERIOD.getCode(), null);
        }

        // 首页 / 时间线
        if (containsAny(m, "首页", "概览", "dashboard")) {
            return IntentRecognitionResult.rule(IntentEnum.QUERY_DASHBOARD.getCode(), null);
        }
        if (containsAny(m, "时间线", "回忆", "timeline", "时光轴")) {
            return IntentRecognitionResult.rule(IntentEnum.QUERY_TIMELINE.getCode(), null);
        }

        // 通知
        if (containsAny(m, "通知", "提醒")) {
            return IntentRecognitionResult.rule(IntentEnum.QUERY_NOTIFICATION.getCode(), null);
        }

        // 保存记忆
        if (containsAny(m, "记住", "别忘了", "我喜欢", "我不喜欢", "我们的")) {
            return IntentRecognitionResult.rule(IntentEnum.SAVE_MEMORY.getCode(), null);
        }

        // 查询上下文
        if (containsAny(m, "刚才", "之前", "你说过", "对话", "聊天记录")) {
            return IntentRecognitionResult.rule(IntentEnum.QUERY_CONTEXT.getCode(), null);
        }

        // 贴纸
        if (containsAny(m, "贴纸", "贴图", "sticker")) {
            return IntentRecognitionResult.rule(IntentEnum.QUERY_STICKER.getCode(), null);
        }

        // 个人资料
        if (containsAny(m, "个人资料", "我的信息", "profile", "昵称", "签名")) {
            if (containsAny(m, "修改", "改", "更新", "换", "设置")) {
                return IntentRecognitionResult.rule(IntentEnum.UPDATE_PROFILE.getCode(), null);
            }
            return IntentRecognitionResult.rule(IntentEnum.QUERY_PROFILE.getCode(), null);
        }

        // 情侣信息
        if (containsAny(m, "情侣", "恋爱", "在一起", "我们")) {
            return IntentRecognitionResult.rule(IntentEnum.QUERY_COUPLE_INFO.getCode(), null);
        }

        // 天气查询
        if (containsAny(m, "天气", "气温", "下雨", "刮风", "多少度", "热不热", "冷不冷", "雾霾")) {
            return IntentRecognitionResult.rule(IntentEnum.QUERY_WEATHER.getCode(), null);
        }

        // 约会规划
        if (containsAny(m, "约会", "去哪玩", "去哪吃", "约会计划", "约会方案", "怎么约会", "推荐约会", "约会去哪")) {
            return IntentRecognitionResult.rule(IntentEnum.PLAN_DATE.getCode(), null);
        }

        // 旅行攻略
        if (containsAny(m, "旅游", "旅行", "攻略", "度假", "出游", "去哪旅游", "旅行计划", "旅游攻略")) {
            return IntentRecognitionResult.rule(IntentEnum.PLAN_TRAVEL.getCode(), null);
        }

        // 兜底: 闲聊
        return IntentRecognitionResult.rule(IntentEnum.CASUAL_CHAT.getCode(), null);
    }

    // ==================== 缓存 ====================

    private IntentRecognitionResult checkCache(Long userId, String message) {
        if (redisTemplate == null) return null;

        try {
            String key = INTENT_CACHE_PREFIX + userId + ":" + hashMessage(message);
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return null;

            return JSON.readValue(json, IntentRecognitionResult.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void cacheResult(Long userId, String message, IntentRecognitionResult result) {
        if (redisTemplate == null || result == null) return;

        try {
            String key = INTENT_CACHE_PREFIX + userId + ":" + hashMessage(message);
            String json = JSON.writeValueAsString(result);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("意图缓存写入失败: {}", e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    private List<VolcengineChatRequest.Tool> getTools() {
        if (cachedTools == null) {
            synchronized (this) {
                if (cachedTools == null) {
                    cachedTools = catalogService.buildTools();
                }
            }
        }
        return cachedTools;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return JSON.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析 LLM arguments 失败: {}", argumentsJson);
            return Collections.emptyMap();
        }
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
            if (kw.contains(".*")) {
                // 简单正则支持
                if (Pattern.compile(kw).matcher(text).find()) return true;
            }
        }
        return false;
    }

    /** 判断是否为纯表情/符号消息（不含中文、英文、数字） */
    private boolean isPureEmojiOrSymbol(String msg) {
        if (msg == null || msg.isEmpty()) return true;
        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            if (Character.isLetterOrDigit(c)) return false;
            // 中文字符范围
            if (c >= 0x4E00 && c <= 0x9FFF) return false;
        }
        return true;
    }

    /** 消息哈希（去空白 + 截断），用于缓存 key */
    private String hashMessage(String msg) {
        String cleaned = msg.trim().replaceAll("\\s+", "");
        if (cleaned.length() > 100) {
            cleaned = cleaned.substring(0, 100);
        }
        return String.valueOf(cleaned.hashCode());
    }
}
