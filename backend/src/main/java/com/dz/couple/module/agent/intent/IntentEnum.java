package com.dz.couple.module.agent.intent;

import java.util.HashMap;
import java.util.Map;

/**
 * 意图枚举 — 覆盖情侣空间全部 19 个业务模块
 * 每个意图包含: 编码、中文描述、所属模块、是否需要LLM提取参数、参数JSON Schema
 */
public enum IntentEnum {

    // ==================== 纪念日 ====================
    QUERY_ANNIVERSARY("QUERY_ANNIVERSARY", "查询纪念日列表", "anniversary"),
    CREATE_ANNIVERSARY("CREATE_ANNIVERSARY", "创建纪念日", "anniversary",
            "{\"type\":\"object\",\"properties\":{\"title\":{\"type\":\"string\",\"description\":\"纪念日名称\"},\"date\":{\"type\":\"string\",\"description\":\"日期 yyyy-MM-dd\"}},\"required\":[\"title\",\"date\"]}"),
    UPCOMING_ANNIVERSARY("UPCOMING_ANNIVERSARY", "查看即将到来的纪念日", "anniversary"),
    DELETE_ANNIVERSARY("DELETE_ANNIVERSARY", "删除纪念日", "anniversary"),

    // ==================== 日记 ====================
    CREATE_DIARY("CREATE_DIARY", "写日记", "diary",
            "{\"type\":\"object\",\"properties\":{\"content\":{\"type\":\"string\",\"description\":\"日记内容\"},\"mood\":{\"type\":\"string\",\"description\":\"心情:开心/难过/平静/生气\"},\"private\":{\"type\":\"boolean\",\"description\":\"是否私密\"}}}"),
    QUERY_DIARY("QUERY_DIARY", "查看日记", "diary"),
    DIARY_STATS("DIARY_STATS", "日记心情统计", "diary"),

    // ==================== 备忘录 ====================
    CREATE_MEMO("CREATE_MEMO", "创建备忘录", "memo",
            "{\"type\":\"object\",\"properties\":{\"title\":{\"type\":\"string\",\"description\":\"标题\"},\"content\":{\"type\":\"string\",\"description\":\"内容\"}}}"),
    QUERY_MEMO("QUERY_MEMO", "查看备忘录", "memo"),
    COMPLETE_MEMO("COMPLETE_MEMO", "完成备忘录", "memo"),

    // ==================== 记账 ====================
    ADD_ACCOUNT("ADD_ACCOUNT", "添加记账", "account",
            "{\"type\":\"object\",\"properties\":{\"amount\":{\"type\":\"number\",\"description\":\"金额\"},\"category\":{\"type\":\"string\",\"description\":\"分类:餐饮/交通/购物/娱乐/居住/通讯/医疗/教育/人情/其他\"},\"remark\":{\"type\":\"string\",\"description\":\"备注\"},\"date\":{\"type\":\"string\",\"description\":\"日期 yyyy-MM-dd\"}}}"),
    QUERY_ACCOUNT("QUERY_ACCOUNT", "查看记账", "account"),
    ACCOUNT_STATS("ACCOUNT_STATS", "记账统计", "account"),

    // ==================== 照片 ====================
    QUERY_PHOTO("QUERY_PHOTO", "查看照片", "photo"),

    // ==================== 相册 ====================
    QUERY_ALBUM("QUERY_ALBUM", "查看相册", "album"),

    // ==================== 消息 ====================
    QUERY_MESSAGE("QUERY_MESSAGE", "查看未读消息", "message"),

    // ==================== 刮刮乐 ====================
    CREATE_WISH_SCRATCH("CREATE_WISH_SCRATCH", "创建刮刮乐", "wish",
            "{\"type\":\"object\",\"properties\":{\"content\":{\"type\":\"string\",\"description\":\"心愿内容,≤50字\"}}}"),
    QUERY_WISH_SCRATCH("QUERY_WISH_SCRATCH", "查看刮刮乐", "wish"),

    // ==================== 心愿清单 ====================
    ADD_WISH("ADD_WISH", "添加心愿", "wishlist",
            "{\"type\":\"object\",\"properties\":{\"content\":{\"type\":\"string\",\"description\":\"心愿内容\"},\"expectedDate\":{\"type\":\"string\",\"description\":\"期望完成日期 yyyy-MM-dd\"},\"priority\":{\"type\":\"string\",\"description\":\"优先级:高/中/低\"}}}"),
    QUERY_WISH("QUERY_WISH", "查看心愿清单", "wishlist"),
    SPIN_WISH("SPIN_WISH", "转盘抽心愿", "wishlist"),

    // ==================== 经期 ====================
    QUERY_PERIOD("QUERY_PERIOD", "查询经期状态", "period"),
    RECORD_PERIOD("RECORD_PERIOD", "记录/更新经期设置", "period"),

    // ==================== 情侣 ====================
    QUERY_COUPLE_INFO("QUERY_COUPLE_INFO", "查询情侣信息", "couple"),
    QUERY_TOGETHER_DAYS("QUERY_TOGETHER_DAYS", "查询在一起多少天", "couple"),
    UPDATE_LOVE_DATE("UPDATE_LOVE_DATE", "更新恋爱日期", "couple"),

    // ==================== 用户 ====================
    QUERY_PROFILE("QUERY_PROFILE", "查看个人资料", "user"),
    UPDATE_PROFILE("UPDATE_PROFILE", "更新个人资料/昵称/签名", "user"),

    // ==================== 首页 ====================
    QUERY_DASHBOARD("QUERY_DASHBOARD", "查看首页概览", "dashboard"),

    // ==================== 通知 ====================
    QUERY_NOTIFICATION("QUERY_NOTIFICATION", "查看通知", "notification"),

    // ==================== 时间线 ====================
    QUERY_TIMELINE("QUERY_TIMELINE", "查看时间线", "timeline"),

    // ==================== 贴纸 ====================
    QUERY_STICKER("QUERY_STICKER", "查看贴纸", "sticker"),

    // ==================== AI 管家自身 ====================
    SAVE_MEMORY("SAVE_MEMORY", "保存记忆/偏好", "agent",
            "{\"type\":\"object\",\"properties\":{\"content\":{\"type\":\"string\",\"description\":\"要记住的内容\"}}}"),
    QUERY_CONTEXT("QUERY_CONTEXT", "查询对话上下文", "agent"),

    // ==================== 生活规划 ====================
    QUERY_WEATHER("QUERY_WEATHER", "查询指定城市的实时天气。用户问天气/气温/下雨/冷不冷时必须调用此函数", "lifeplan",
            "{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\",\"description\":\"城市名\"}},\"required\":[\"city\"]}"),
    PLAN_DATE("PLAN_DATE", "生成约会计划/推荐约会方案。用户问去哪玩/约会/约会方案时必须调用此函数", "lifeplan",
            "{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\",\"description\":\"约会城市名\"}},\"required\":[\"city\"]}"),
    PLAN_TRAVEL("PLAN_TRAVEL", "制定旅游攻略/旅行计划。用户问旅游/旅行/攻略/出游时必须调用此函数", "lifeplan",
            "{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\",\"description\":\"旅游目的地城市名\"}},\"required\":[\"city\"]}"),

    // ==================== 通用 ====================
    GREETING("GREETING", "问候/打招呼", "general"),
    LOVE_EXPRESSION("LOVE_EXPRESSION", "表达爱意/撒娇", "general"),
    HELP("HELP", "询问AI能做什么", "general"),
    CASUAL_CHAT("CASUAL_CHAT", "闲聊/其他", "general"),
    UNKNOWN("UNKNOWN", "无法识别", "general");

    // ==================== 字段 ====================
    private final String code;
    private final String description;
    private final String category;
    private final String paramSchema; // JSON Schema for function calling, null if no params needed

    private static final Map<String, IntentEnum> BY_CODE = new HashMap<>();
    static {
        for (IntentEnum intent : values()) {
            BY_CODE.put(intent.code, intent);
        }
    }

    IntentEnum(String code, String description, String category) {
        this(code, description, category, null);
    }

    IntentEnum(String code, String description, String category, String paramSchema) {
        this.code = code;
        this.description = description;
        this.category = category;
        this.paramSchema = paramSchema;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getParamSchema() { return paramSchema; }
    public boolean hasParams() { return paramSchema != null && !paramSchema.isEmpty(); }

    public static IntentEnum fromCode(String code) {
        IntentEnum intent = BY_CODE.get(code);
        return intent != null ? intent : UNKNOWN;
    }
}
