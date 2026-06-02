package com.dz.couple.module.agent.intent.handler;

import com.dz.couple.module.agent.dto.AgentChatResponse;
import com.dz.couple.module.agent.intent.IntentEnum;
import com.dz.couple.module.message.service.MessageService;
import com.dz.couple.module.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CommunicationIntentHandler implements IntentHandler {

    private static final Set<IntentEnum> SUPPORTED = EnumSet.of(
            IntentEnum.QUERY_MESSAGE,
            IntentEnum.QUERY_NOTIFICATION
    );

    @Autowired
    private MessageService messageService;

    @Autowired
    private NotificationService notificationService;

    @Override
    public Set<IntentEnum> supportedIntents() {
        return SUPPORTED;
    }

    @Override
    public AgentChatResponse handle(Long userId, Long coupleId, String message, Map<String, Object> params) {
        List<String> actions = new ArrayList<>();
        String reply;

        if (message.contains("消息") || message.contains("聊天") || message.contains("未读")) {
            int unread = messageService.countUnread(userId, coupleId);
            if (unread > 0) {
                reply = "你有 " + unread + " 条未读消息~ 💬\n快去聊天页面看看吧！";
            } else {
                reply = "没有未读消息~ 要不要给TA发一条消息？💬";
            }
            actions.add("查询消息");

        } else if (message.contains("通知") || message.contains("提醒")) {
            int unread = notificationService.countUnreadAll(userId);
            if (unread > 0) {
                reply = "有 " + unread + " 条未读通知~ 🔔\n去通知页面看看吧！";
            } else {
                reply = "没有未读通知~ 一切安好！✨";
            }
            actions.add("查询通知");

        } else {
            reply = "去看看消息和通知吧~ 可能有TA给你的惊喜哦！💕";
        }

        AgentChatResponse resp = new AgentChatResponse();
        resp.setReply(reply);
        resp.setEmotion("中立");
        resp.setExecutedActions(actions);
        resp.setNeedFollowUp(false);
        return resp;
    }
}
