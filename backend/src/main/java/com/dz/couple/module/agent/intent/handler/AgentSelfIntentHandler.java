package com.dz.couple.module.agent.intent.handler;

import com.dz.couple.module.agent.dto.AgentChatResponse;
import com.dz.couple.module.agent.intent.IntentEnum;
import com.dz.couple.module.agent.memory.ContextMemoryService;
import com.dz.couple.module.agent.memory.MemoryManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AgentSelfIntentHandler implements IntentHandler {

    private static final Set<IntentEnum> SUPPORTED = EnumSet.of(
            IntentEnum.SAVE_MEMORY,
            IntentEnum.QUERY_CONTEXT
    );

    @Autowired
    private MemoryManagerService memoryManager;

    @Autowired
    private ContextMemoryService contextMemoryService;

    /** 常见中国城市名，用于自动识别 */
    private static final String[] CITY_NAMES = {
        "北京","上海","广州","深圳","杭州","成都","重庆","南京","武汉",
        "西安","长沙","厦门","三亚","大理","丽江","苏州","青岛","大连","哈尔滨",
        "昆明","贵阳","拉萨","桂林","北海","海口","郑州","天津","济南","沈阳",
        "合肥","福州","南昌","南宁","银川","西宁","兰州","乌鲁木齐","呼和浩特","石家庄"
    };

    @Override
    public Set<IntentEnum> supportedIntents() {
        return SUPPORTED;
    }

    @Override
    public AgentChatResponse handle(Long userId, Long coupleId, String message, Map<String, Object> params) {
        List<String> actions = new ArrayList<>();
        String reply;

        if (message.contains("记住") || message.contains("别忘了") || message.contains("我喜欢")
                || message.contains("我不喜欢") || params.containsKey("content")
                || containsCityPattern(message)) {

            String content = paramStr(params, "content");
            if (content == null) {
                content = message.replaceFirst("^.*(帮我)?记住[：:]?\\s*", "")
                        .replaceFirst("^别忘了[：:]?\\s*", "")
                        .replaceFirst("^记得[：:]?\\s*", "")
                        .trim();
            }

            // 自动识别城市信息：用户在/住在/现在在 XX → 保存为城市偏好
            String detectedCity = detectCity(message);
            if (detectedCity != null) {
                memoryManager.saveCoreMemory(coupleId, "PREFERENCE", "city",
                        detectedCity, 3, "USER_INPUT");
                memoryManager.saveSceneMemory(coupleId, userId, "ACTION",
                        "用户所在城市：" + detectedCity, null, "SAVE_MEMORY", 3);
                reply = "好的，记住你们在" + detectedCity + "啦~ 🌍 以后查天气、做约会攻略都会优先用" + detectedCity + "！";
                actions.add("保存城市");
                AgentChatResponse resp = new AgentChatResponse();
                resp.setReply(reply);
                resp.setEmotion("开心");
                resp.setExecutedActions(actions);
                resp.setNeedFollowUp(false);
                return resp;
            }

            if (content.isEmpty() && detectedCity == null) {
                reply = "好的，你想让我记住什么呢？比如：「记住我不喜欢吃辣」或者告诉我「我在广州」💭";
            } else {
                String memoryType;
                String memoryKey;
                int importance = 3;

                if (content.contains("不喜欢") || content.contains("讨厌")) {
                    memoryType = "PREFERENCE";
                    memoryKey = "dislike_" + System.currentTimeMillis();
                    importance = 4;
                } else if (content.contains("喜欢") || content.contains("爱")) {
                    memoryType = "PREFERENCE";
                    memoryKey = "like_" + System.currentTimeMillis();
                    importance = 4;
                } else {
                    memoryType = "CUSTOM";
                    memoryKey = "custom_" + System.currentTimeMillis();
                }

                memoryManager.saveCoreMemory(coupleId, memoryType, memoryKey, content, importance, "USER_INPUT");
                memoryManager.saveSceneMemory(coupleId, userId, "ACTION",
                        "用户要求记住：" + content, null, "SAVE_MEMORY", 3);

                reply = "好的，我记住了~ 🧠✨\n\n【已记住】" + content + "\n\n以后我会更懂你们的！💕";
                actions.add("保存记忆");
            }

        } else {
            // 查询上下文
            List<Map<String, Object>> dialogues = contextMemoryService.getRecentDialogues(userId, 3);
            if (dialogues.isEmpty()) {
                reply = "我们还没有开始对话呢~ 想聊点什么？💬";
            } else {
                StringBuilder sb = new StringBuilder("📝 最近的对话记录：\n\n");
                for (int i = 0; i < Math.min(3, dialogues.size()); i++) {
                    Map<String, Object> d = dialogues.get(i);
                    sb.append("💬 你: ").append(d.get("userMessage")).append("\n");
                    sb.append("🤖 我: ").append(d.get("agentResponse")).append("\n\n");
                }
                reply = sb.toString();
            }
            actions.add("查询上下文");
        }

        AgentChatResponse resp = new AgentChatResponse();
        resp.setReply(reply);
        resp.setEmotion("开心");
        resp.setExecutedActions(actions);
        resp.setNeedFollowUp(false);
        return resp;
    }

    private String paramStr(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v != null ? v.toString() : null;
    }

    /** 检测消息是否包含城市声明模式 */
    private boolean containsCityPattern(String message) {
        return message.contains("我在") || message.contains("我住在")
            || message.contains("现在在") || message.contains("我们在");
    }

    /** 从消息中提取城市名 */
    private String detectCity(String message) {
        for (String city : CITY_NAMES) {
            if (message.contains(city)) return city;
        }
        return null;
    }
}
