package com.dz.couple.module.agent.intent;

import com.dz.couple.module.agent.memory.MemoryManagerService;
import com.dz.couple.module.agent.volcengine.VolcengineChatRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 意图目录服务 — 构建 LLM Function Calling 的工具定义和系统提示词
 * 将 IntentEnum 中的全部业务意图转化为 OpenAI Function Calling 格式
 */
@Service
public class IntentCatalogService {

    private static final Logger log = LoggerFactory.getLogger(IntentCatalogService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MemoryManagerService memoryManager;

    /**
     * 构建系统提示词 — 支持「业务功能调用」+「自然对话」双模式
     *
     * @param coupleId 情侣ID（用于查询基础记忆）
     * @param conversationHistory 最近对话历史文本，可为空
     */
    public String buildSystemPrompt(Long coupleId, String conversationHistory) {
        StringBuilder sb = new StringBuilder();

        // ===== 人设 =====
        sb.append("你是「小爱」，一对甜蜜情侣的专属AI管家。\n");
        sb.append("你的性格：温柔贴心、幽默俏皮，像他们的闺蜜/兄弟一样自然聊天。\n\n");

        // ===== 核心行为规则 =====
        sb.append("## 行为决策（按优先级判断）\n");
        sb.append("1. 先判断用户是否在聊天：表情包、单个emoji、问候语、撒娇、倾诉、闲聊 → 直接生成文字回复，绝对不调用 function\n");
        sb.append("2. 用户明确说出具体操作需求时，才调用 function：\n");
        sb.append("   - 查天气/约会/旅游 → 调用 QUERY_WEATHER / PLAN_DATE / PLAN_TRAVEL\n");
        sb.append("   - 记账/写日记/备忘录/纪念日/心愿/经期 → 调用对应的 function\n");
        sb.append("3. 拿不准的时候：优先直接文字回复，不要硬选一个 function 调用\n\n");

        // ===== 回复风格 =====
        sb.append("## 回复风格\n");
        sb.append("- 口语化，像微信聊天，不要客服腔、不要机器人腔\n");
        sb.append("- 每条回复2-4句话，简洁利落\n");
        sb.append("- emoji适量点缀（1-2个），别刷屏\n");
        sb.append("- 每次回复换个说法，别当复读机\n");
        sb.append("- 带点情侣间的小调侃、小撒娇\n");
        sb.append("- **禁止**使用以下词语：根据我的分析、作为AI、系统显示、基于数据、经查询\n\n");

        // ===== 情侣信息 =====
        String memoryCtx = memoryManager.buildMemoryContext(coupleId);
        if (memoryCtx != null && !memoryCtx.trim().isEmpty()) {
            sb.append("## 关于这对情侣\n");
            sb.append(memoryCtx).append("\n");
            sb.append("（在对话中自然地使用以上信息，让回复和他们的生活相关，但不要刻意罗列）\n\n");
        }

        // ===== 对话历史 =====
        if (conversationHistory != null && !conversationHistory.trim().isEmpty()) {
            sb.append("## 刚才聊了什么\n");
            sb.append(conversationHistory).append("\n\n");
        }

        sb.append("现在，开始和这对可爱的人对话吧~");
        return sb.toString();
    }

    /**
     * 构建全部可用工具(Function)定义列表
     * 只包含「业务操作类」意图，闲聊类（问候/爱意/帮助/闲聊）由 LLM 直接回复
     */
    public List<VolcengineChatRequest.Tool> buildTools() {
        List<VolcengineChatRequest.Tool> tools = new ArrayList<>();

        for (IntentEnum intent : IntentEnum.values()) {
            // 跳过无法识别 + 闲聊类意图（这些由 LLM 直接生成文本回复，不需要 function calling）
            if (intent == IntentEnum.UNKNOWN
                    || intent == IntentEnum.GREETING
                    || intent == IntentEnum.LOVE_EXPRESSION
                    || intent == IntentEnum.HELP
                    || intent == IntentEnum.CASUAL_CHAT) {
                continue;
            }

            VolcengineChatRequest.Function func = new VolcengineChatRequest.Function();
            func.setName(intent.getCode());
            func.setDescription(buildDescription(intent));

            // 构建 parameters schema
            if (intent.hasParams()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> schema = JSON.readValue(intent.getParamSchema(), Map.class);
                    func.setParameters(schema);
                } catch (JsonProcessingException e) {
                    // 无参数 schema
                    Map<String, Object> emptyParams = new LinkedHashMap<>();
                    emptyParams.put("type", "object");
                    emptyParams.put("properties", new LinkedHashMap<>());
                    func.setParameters(emptyParams);
                }
            } else {
                Map<String, Object> emptyParams = new LinkedHashMap<>();
                emptyParams.put("type", "object");
                emptyParams.put("properties", new LinkedHashMap<>());
                func.setParameters(emptyParams);
            }

            tools.add(new VolcengineChatRequest.Tool(func));
        }

        log.info("已构建 {} 个工具定义", tools.size());
        return tools;
    }

    /**
     * 构建精简功能描述
     */
    private String buildDescription(IntentEnum intent) {
        String example = getExample(intent);
        if (example != null) {
            return intent.getDescription() + "。如：「" + example + "」";
        }
        return intent.getDescription();
    }

    private String getExample(IntentEnum intent) {
        switch (intent) {
            case QUERY_ANNIVERSARY: return "我们的纪念日还有多久 / 最近有什么纪念日";
            case CREATE_ANNIVERSARY: return "帮我记录一个纪念日：第一次见面 2024-01-15";
            case UPCOMING_ANNIVERSARY: return "下一个纪念日是什么";
            case CREATE_DIARY: return "帮我写日记 / 今天心情很好";
            case QUERY_DIARY: return "最近写了哪些日记";
            case DIARY_STATS: return "最近心情怎么样 / 这个月什么心情最多";
            case CREATE_MEMO: return "提醒我明天买花 / 帮我建个备忘录";
            case QUERY_MEMO: return "看看我的备忘录 / 还有哪些待办";
            case COMPLETE_MEMO: return "这个备忘录完成了";
            case ADD_ACCOUNT: return "今天吃饭花了100块 / 记账";
            case QUERY_ACCOUNT: return "看看最近的账单 / 这个月花了多少";
            case ACCOUNT_STATS: return "这个月开销统计 / 哪类花得最多";
            case QUERY_PHOTO: return "看看我们的照片 / 最近上传的照片";
            case QUERY_ALBUM: return "有哪些相册";
            case QUERY_MESSAGE: return "有没有新消息";
            case CREATE_WISH_SCRATCH: return "帮我做个刮刮乐";
            case QUERY_WISH_SCRATCH: return "看看刮刮乐 / 有什么惊喜";
            case ADD_WISH: return "添加心愿：一起去海边";
            case QUERY_WISH: return "我的心愿清单";
            case SPIN_WISH: return "转转盘 / 抽个心愿";
            case QUERY_PERIOD: return "姨妈什么时候来 / 经期还有几天";
            case RECORD_PERIOD: return "记录经期 / 大姨妈来了";
            case QUERY_COUPLE_INFO: return "我们是谁 / 查询情侣信息";
            case QUERY_TOGETHER_DAYS: return "我们在一起多少天了";
            case QUERY_DASHBOARD: return "首页概览 / 今天有什么";
            case QUERY_NOTIFICATION: return "有什么通知 / 查看通知";
            case QUERY_TIMELINE: return "时间线 / 我们的回忆";
            case QUERY_STICKER: return "有什么贴纸";
            case SAVE_MEMORY: return "记住我不喜欢吃香菜";
            case QUERY_CONTEXT: return "刚才我们说了什么";
            case QUERY_WEATHER: return "今天天气怎么样 / 查一下杭州天气";
            case PLAN_DATE: return "帮我做个约会计划 / 周末去哪玩 / 推荐约会方案";
            case PLAN_TRAVEL: return "做个去三亚的旅游攻略 / 厦门旅行计划";
            case GREETING: return "你好 / hi / 在吗";
            case LOVE_EXPRESSION: return "爱你 / 想你";
            case HELP: return "你能做什么 / 有什么功能";
            default: return null;
        }
    }
}
