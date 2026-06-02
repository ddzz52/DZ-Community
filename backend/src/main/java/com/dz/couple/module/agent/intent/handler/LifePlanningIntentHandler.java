package com.dz.couple.module.agent.intent.handler;

import com.dz.couple.module.agent.dto.AgentChatResponse;
import com.dz.couple.module.agent.intent.IntentEnum;
import com.dz.couple.module.agent.weather.WeatherClient;
import com.dz.couple.module.agent.intent.IntentCatalogService;
import com.dz.couple.module.agent.intent.IntentRecognitionService;
import com.dz.couple.module.agent.memory.MemoryManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LifePlanningIntentHandler implements IntentHandler {

    private static final Logger log = LoggerFactory.getLogger(LifePlanningIntentHandler.class);
    private static final Random RNG = new Random();

    private static final Set<IntentEnum> SUPPORTED = EnumSet.of(
            IntentEnum.QUERY_WEATHER,
            IntentEnum.PLAN_DATE,
            IntentEnum.PLAN_TRAVEL
    );

    @Autowired
    private WeatherClient weatherClient;

    @Autowired
    private MemoryManagerService memoryManager;

    @Autowired
    private IntentCatalogService intentCatalogService;

    @Autowired
    private IntentRecognitionService intentRecognitionService;

    @Override
    public Set<IntentEnum> supportedIntents() {
        return SUPPORTED;
    }

    @Override
    public AgentChatResponse handle(Long userId, Long coupleId, String message, Map<String, Object> params) {
        // 第一步：提取城市（消息中的城市 > 核心记忆中的城市 > 询问用户）
        String city = extractCity(message, params);
        if (city == null || city.trim().isEmpty()) {
            city = memoryManager.getCoreMemoryValue(coupleId, "PREFERENCE", "city");
        }
        if (city == null || city.trim().isEmpty()) {
            AgentChatResponse resp = new AgentChatResponse();
            resp.setReply("我还不知道你们在哪个城市呢~ 告诉我城市名，我帮你查天气、做约会计划！比如「我在广州」🌍");
            resp.setEmotion("好奇");
            resp.setExecutedActions(Collections.emptyList());
            resp.setNeedFollowUp(true);
            resp.setFollowUpSuggestion("你在哪个城市呀？");
            return resp;
        }
        city = city.trim();

        // 第二步：确定子意图（_intent 由 IntentDispatchService 保证注入，100% 不为空）
        String intentCode = "";
        if (params != null) {
            Object intentVal = params.get("_intent");
            if (intentVal != null) {
                intentCode = intentVal.toString();
            }
        }
        // 兜底：仅当 _intent 真的缺失时，用消息文本推断
        if (intentCode.isEmpty()) {
            intentCode = detectIntent(message);
        }

        log.info("LifePlanning 子意图分发: intentCode={}, city={}, message={}", intentCode, city, message);

        // 第三步：按意图分发
        AgentChatResponse resp = new AgentChatResponse();
        switch (intentCode) {
            case "PLAN_DATE":
                return handleDatePlan(userId, coupleId, city, resp);
            case "PLAN_TRAVEL":
                return handleTravelPlan(userId, coupleId, city, message, resp);
            case "QUERY_WEATHER":
                return handleWeather(city, resp);
            default:
                // 无法识别的子意图 → 不默认查天气，反问用户
                resp.setReply("你是想查天气、做约会计划、还是做旅游攻略呀？告诉我具体需求，我帮你安排~ 💕");
                resp.setEmotion("好奇");
                resp.setExecutedActions(Collections.emptyList());
                resp.setNeedFollowUp(true);
                return resp;
        }
    }

    /** 天气查询 */
    private AgentChatResponse handleWeather(String city, AgentChatResponse resp) {
        Map<String, Object> weather = weatherClient.query(city);
        List<String> actions = new ArrayList<>();
        actions.add("查询天气");

        if (weather == null) {
            resp.setReply(sunnyFallback(city));
            resp.setEmotion("开心");
            resp.setExecutedActions(actions);
            return resp;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("「").append(city).append("」天气来啦~ ☀️\n\n");
        sb.append("🌡 当前温度：").append(weather.get("temp")).append("°C");
        String feels = String.valueOf(weather.get("feelsLike"));
        if (!feels.equals(weather.get("temp"))) {
            sb.append("（体感 ").append(feels).append("°C）");
        }
        sb.append("\n☁️ 天气：").append(weather.get("desc"));
        sb.append("\n💧 湿度：").append(weather.get("humidity")).append("%");
        sb.append("\n🌬 风速：").append(weather.get("windSpeed")).append(" ").append(weather.get("windDir"));

        @SuppressWarnings("unchecked")
        Map<String, String> forecast = (Map<String, String>) weather.get("forecast");
        if (forecast != null && !forecast.isEmpty()) {
            sb.append("\n\n📅 未来几天：");
            for (Map.Entry<String, String> e : forecast.entrySet()) {
                sb.append("\n  ").append(e.getKey()).append("  ").append(e.getValue());
            }
        }

        // 加一句恋人关怀
        sb.append("\n\n").append(weatherAdvice(String.valueOf(weather.get("desc")), city));

        resp.setReply(sb.toString());
        resp.setEmotion("开心");
        resp.setExecutedActions(actions);
        return resp;
    }

    /** 约会计划 */
    private AgentChatResponse handleDatePlan(Long userId, Long coupleId, String city, AgentChatResponse resp) {
        Map<String, Object> weather = weatherClient.query(city);
        List<String> actions = new ArrayList<>();
        actions.add("制定约会计划");

        StringBuilder sb = new StringBuilder();
        sb.append("💕 你们的专属约会计划来啦~\n\n");

        if (weather != null) {
            sb.append("📌 参考天气：").append(city).append(" ")
                    .append(weather.get("temp")).append("°C，").append(weather.get("desc")).append("\n\n");
        }

        // 根据天气类型生成不同的约会建议
        String desc = weather != null ? String.valueOf(weather.get("desc")) : "";
        String tempStr = weather != null ? String.valueOf(weather.get("temp")) : "20";
        double temp = Double.parseDouble(tempStr);
        String plan = generateDatePlan(desc, temp, city);
        sb.append(plan);

        resp.setReply(sb.toString());
        resp.setEmotion("开心");
        resp.setExecutedActions(actions);
        return resp;
    }

    /** 旅游攻略 */
    private AgentChatResponse handleTravelPlan(Long userId, Long coupleId, String city, String message, AgentChatResponse resp) {
        Map<String, Object> weather = weatherClient.query(city);
        List<String> actions = new ArrayList<>();
        actions.add("生成旅游攻略");

        StringBuilder sb = new StringBuilder();
        sb.append("🗺️ ").append(city).append(" 情侣旅游攻略~\n\n");

        if (weather != null) {
            sb.append("🌤 当地天气：").append(weather.get("temp")).append("°C，").append(weather.get("desc")).append("\n\n");
        }

        sb.append(generateTravelGuide(city, message));
        resp.setReply(sb.toString());
        resp.setEmotion("开心");
        resp.setExecutedActions(actions);
        return resp;
    }

    // ==================== 辅助方法 ====================

    private String extractCity(String message, Map<String, Object> params) {
        if (params != null && params.containsKey("city")) {
            return String.valueOf(params.get("city")).trim();
        }
        // 从消息中提取城市名（简单规则）
        String[] suffixes = {"天气", "约会", "旅游", "攻略", "旅行", "玩", "出行"};
        for (String sfx : suffixes) {
            int idx = message.indexOf(sfx);
            if (idx > 0) {
                String before = message.substring(0, idx).trim();
                // 取最后2-4个字符作为城市名
                if (before.length() >= 2) {
                    String candidate = before.substring(Math.max(0, before.length() - 4));
                    // 去掉常见的前缀词
                    candidate = candidate.replaceAll("[的去在到给帮查看看一下]", "").trim();
                    if (!candidate.isEmpty() && candidate.length() <= 6) return candidate;
                }
            }
        }
        // 检查消息中是否有"北京/上海/广州/深圳/杭州/成都/重庆/南京/武汉/西安/长沙/厦门/三亚/大理/丽江/苏州/青岛/大连/哈尔滨/昆明/贵阳/拉萨/桂林/北海/海口/三亚"等城市名
        String[] cities = {"北京","上海","广州","深圳","杭州","成都","重庆","南京","武汉",
                "西安","长沙","厦门","三亚","大理","丽江","苏州","青岛","大连","哈尔滨",
                "昆明","贵阳","拉萨","桂林","北海","海口","郑州","天津","济南","沈阳","合肥","福州","南昌"};
        for (String c : cities) {
            if (message.contains(c)) return c;
        }
        return null;
    }

    private String detectIntent(String message) {
        if (containsAny(message, "约会", "date", "去哪里玩", "去哪玩", "出去玩", "安排")) return "PLAN_DATE";
        if (containsAny(message, "旅游", "旅行", "攻略", "出游", "景点")) return "PLAN_TRAVEL";
        if (containsAny(message, "天气", "气温", "下雨", "多少度", "冷不冷", "热不热", "刮风")) return "QUERY_WEATHER";
        // 无法判断时返回空字符串，由调用方反问用户，绝不默认查天气
        return "";
    }

    private String weatherAdvice(String desc, String city) {
        if (containsAny(desc, "雨", "雪", "storm", "rain")) {
            return "下雨天也是约会的好天气呢~ 带上伞，和TA一起雨中漫步也很浪漫 ☂️";
        }
        if (containsAny(desc, "晴", "sunny", "clear")) {
            return "天气超棒！适合和TA一起出去走走，来个阳光下的约会吧 ☀️💕";
        }
        if (containsAny(desc, "阴", "多云", "cloud")) {
            return "天气还不错哦~ 不冷不热的，很适合和TA出门逛逛街 🛍️";
        }
        return "不管什么天气，和TA在一起就是最好的天气~ ✨";
    }

    private String sunnyFallback(String city) {
        String[] fallbacks = {
                "虽然没能查到" + city + "的实时天气，但不管外面什么天气，和TA在一起心里都是大晴天呀 ☀️💕",
                city + "的天气信号暂时没接收到~ 不过没关系，反正你们在一起的时候，哪里都是好天气 ✨",
                "哎呀，" + city + "的天气数据迷路啦 🕊️ 不如直接看看窗外？或者换个城市试试告诉我~"
        };
        return fallbacks[RNG.nextInt(fallbacks.length)];
    }

    private String generateDatePlan(String weatherDesc, double temp, String city) {
        StringBuilder plan = new StringBuilder();

        if (temp < 10) {
            plan.append("🧣 天冷约会方案：\n");
            plan.append("• 一起去温暖的咖啡馆，点两杯热巧克力 ☕\n");
            plan.append("• 逛室内集市或美术馆，手牵手慢慢走 🎨\n");
            plan.append("• 晚上吃火锅，暖暖的好幸福 🍲\n");
            plan.append("• 回家窝在沙发看一部爱情电影 🎬");
        } else if (temp > 30) {
            plan.append("🌊 夏日约会方案：\n");
            plan.append("• 去海边/水上乐园，清凉一整天的快乐 🏖️\n");
            plan.append("• 傍晚逛夜市，吃冰淇淋、抓娃娃 🍦\n");
            plan.append("• 星空下散步，聊聊你们的未来 ✨\n");
            plan.append("• 或者去有空调的电影院看新片 🎥");
        } else {
            plan.append("🌸 完美天气约会方案：\n");
            plan.append("• 上午去公园野餐，带上水果和三明治 🧺\n");
            plan.append("• 下午逛逛" + city + "的特色街区，拍照打卡 📸\n");
            plan.append("• 傍晚找一家有露台的餐厅，边吃边看日落 🌅\n");
            plan.append("• 晚上去清吧听现场音乐，微醺的浪漫 🍷");
        }

        if (containsAny(weatherDesc, "雨", "rain")) {
            plan.append("\n\n🌧 虽然下雨，但也有雨中浪漫：\n");
            plan.append("• 去室内游乐场/蹦床公园尽情玩耍 🎪\n");
            plan.append("• 一起做手工/烘焙，亲手给TA做个小蛋糕 🎂");
        }

        plan.append("\n\n最重要的是：和TA在一起的每一刻都值得珍惜 💝");
        return plan.toString();
    }

    private String generateTravelGuide(String city, String message) {
        StringBuilder guide = new StringBuilder();

        // 根据城市生成基础攻略
        guide.append("📍 推荐游玩天数：2-3天\n\n");

        guide.append("🏨 住宿建议：\n");
        guide.append("• 选择市中心或景区附近的民宿，方便出行\n");
        guide.append("• 情侣推荐有落地窗/浴缸/阳台的房型，氛围感满满 ✨\n\n");

        guide.append("🎯 必打卡景点：\n");
        guide.append("（具体景点因城市而异，建议出发前查一下当地热门打卡地哦~）\n");
        guide.append("• 当地地标景点 — 留下你们的合照 📸\n");
        guide.append("• 一条美食街 — 牵着手边走边吃 🍜\n");
        guide.append("• 一个可以看日落/夜景的地方 — 浪漫时刻 🌅\n\n");

        guide.append("🍽 美食推荐：\n");
        guide.append("• 一定要试试" + city + "本地特色小吃！\n");
        guide.append("• 找一家评分高的本地餐厅，避开景区宰客店 🙂\n");
        guide.append("• 别忘了拍照记录你们的「美食地图」🗺️\n\n");

        guide.append("💡 情侣出行小贴士：\n");
        guide.append("• 提前查好天气，带合适的衣服和雨具\n");
        guide.append("• 行程不要排太满，留点时间随性漫步\n");
        guide.append("• 给TA准备一个小惊喜（比如突然拿出准备好的纪念日礼物）🎁\n");
        guide.append("• 用咱家 App 记录旅途中的美好瞬间~ ✍️\n\n");

        guide.append("愿你们的每一次旅行都是最美的回忆 💕✈️");

        return guide.toString();
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
