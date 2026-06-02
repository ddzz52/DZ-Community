package com.dz.couple.module.agent.intent.handler;

import com.dz.couple.module.agent.dto.AgentChatResponse;
import com.dz.couple.module.agent.intent.IntentEnum;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GeneralIntentHandler implements IntentHandler {

    private static final Set<IntentEnum> SUPPORTED = EnumSet.of(
            IntentEnum.GREETING,
            IntentEnum.LOVE_EXPRESSION,
            IntentEnum.HELP,
            IntentEnum.CASUAL_CHAT,
            IntentEnum.UNKNOWN
    );

    private static final String[] GREETINGS = {
            "你好呀~ 今天想聊点什么呢？💕",
            "嗨！我在这里哦~ 有什么需要帮忙的吗？😊",
            "你们好呀！今天也是充满爱的一天~ 💝",
            "Hi~ 小爱随时为你服务！想查纪念日、写日记还是记账？✨"
    };

    private static final String[] LOVE_REPLIES = {
            "我也爱你们~ 真为你们开心！💕✨",
            "甜度超标啦~ 要一直幸福下去哦！🥰",
            "感受到你们的爱了~ 祝你们永远甜蜜！💝",
            "双向奔赴的爱情最美好啦~ 你们好甜！🍬"
    };

    private static final String HELP_TEXT =
            "我是小爱，你们的情侣AI管家~ 💕\n\n"
                    + "我可以帮你们：\n"
                    + "🌤 天气查询 — 查任何城市的实时天气\n"
                    + "💕 约会计划 — 根据天气定制约会方案\n"
                    + "🗺️ 旅游攻略 — 制定情侣旅行计划\n"
                    + "📅 纪念日 — 查询、创建纪念日提醒\n"
                    + "📝 日记 — 写日记、查日记、心情统计\n"
                    + "📌 备忘录 — 创建、查询备忘和提醒\n"
                    + "💰 记账 — 记账、查账单、月度统计\n"
                    + "📸 相册 — 查看照片和相册\n"
                    + "🎁 刮刮乐 — 制作和查看刮刮乐\n"
                    + "✨ 心愿清单 — 添加心愿、转盘抽心愿\n"
                    + "📅 经期关怀 — 查询经期状态和提醒\n"
                    + "💑 情侣信息 — 查看在一起天数\n"
                    + "🧠 记忆 — 记住你们的偏好和习惯\n\n"
                    + "试试告诉我你想做什么吧~ 😊";

    private static final String[] CASUAL_REPLIES = {
            "我还在不断学习中~ 你可以让我帮你查纪念日、写日记、创建备忘录、记账等 💕\n试试对我说「你能做什么」了解更多功能~",
            "这个我还不太明白，但我可以帮你：查纪念日、写日记、记账、做刮刮乐~ 试试告诉我具体需求？😊",
            "我主要帮助你们记录美好时光~ 要不要试试让我帮你记点什么？📝\n发送「帮助」查看我能做的所有事情~"
    };

    @Override
    public Set<IntentEnum> supportedIntents() {
        return SUPPORTED;
    }

    @Override
    public AgentChatResponse handle(Long userId, Long coupleId, String message, Map<String, Object> params) {
        String reply;
        String emotion = "开心";
        List<String> actions = new ArrayList<>();

        if (containsAny(message.toLowerCase(), "你好", "hi", "hello", "在吗", "嗨", "早啊", "晚上好")) {
            reply = GREETINGS[new Random().nextInt(GREETINGS.length)];
            actions.add("问候");

        } else if (containsAny(message, "爱你", "想你", "喜欢你", "么么哒", "抱抱")) {
            reply = LOVE_REPLIES[new Random().nextInt(LOVE_REPLIES.length)];
            actions.add("爱意表达");

        } else if (containsAny(message, "你能做什么", "有什么功能", "你能干嘛", "help", "怎么用", "帮助")) {
            reply = HELP_TEXT;
            actions.add("帮助");

        } else {
            // 默认闲聊
            reply = CASUAL_REPLIES[new Random().nextInt(CASUAL_REPLIES.length)];
            emotion = "中立";
            actions.add("闲聊");
        }

        AgentChatResponse resp = new AgentChatResponse();
        resp.setReply(reply);
        resp.setEmotion(emotion);
        resp.setExecutedActions(actions);
        resp.setNeedFollowUp(false);
        return resp;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
