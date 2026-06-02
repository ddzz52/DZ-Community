package com.dz.couple.module.agent.intent.handler;

import com.dz.couple.module.agent.dto.AgentChatResponse;
import com.dz.couple.module.agent.intent.IntentEnum;
import com.dz.couple.module.period.dto.PeriodPredictionVO;
import com.dz.couple.module.period.dto.PeriodSettingsVO;
import com.dz.couple.module.period.dto.PeriodStatusVO;
import com.dz.couple.module.period.dto.UpdatePeriodSettingsRequest;
import com.dz.couple.module.period.service.PeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PeriodIntentHandler implements IntentHandler {

    private static final Set<IntentEnum> SUPPORTED = EnumSet.of(
            IntentEnum.QUERY_PERIOD,
            IntentEnum.RECORD_PERIOD
    );

    @Autowired
    private PeriodService periodService;

    @Override
    public Set<IntentEnum> supportedIntents() {
        return SUPPORTED;
    }

    @Override
    public AgentChatResponse handle(Long userId, Long coupleId, String message, Map<String, Object> params) {
        List<String> actions = new ArrayList<>();
        String reply;

        if (message.contains("记录") || message.contains("设置") || message.contains("更新") || message.contains("来了")) {
            reply = "经期设置请到经期关怀页面操作哦~ 在那里可以记录上次经期、设置周期等 📅\n"
                    + "设置好后我会提前提醒你们的！💕";
            actions.add("引导经期设置");

        } else {
            // 查询经期状态
            try {
                PeriodStatusVO status = periodService.getStatus(coupleId);
                PeriodSettingsVO settings = status.getSettings();
                PeriodPredictionVO prediction = status.getPrediction();

                if (settings == null || prediction == null) {
                    reply = "还没有设置经期信息哦~ 去经期关怀页面设置一下吧，我会帮你们做好提醒！📅";
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("📅 经期状态：\n\n");
                    sb.append("当前阶段：").append(prediction.getPhase()).append("\n");
                    sb.append("周期天数：").append(settings.getCycleDays()).append("天\n");
                    sb.append("经期天数：").append(settings.getPeriodDays()).append("天\n");
                    sb.append("下次经期：").append(prediction.getNextPeriodStart())
                            .append(" ~ ").append(prediction.getNextPeriodEnd()).append("\n");
                    sb.append("还有 ").append(prediction.getDaysToNextPeriod()).append(" 天\n\n");

                    if (prediction.getDaysToNextPeriod() <= 3) {
                        sb.append("💡 提醒：经期快到了，注意保暖和休息哦~");
                    } else if ("排卵期".equals(prediction.getPhase())) {
                        sb.append("💡 当前处于排卵期~");
                    }

                    reply = sb.toString();
                    actions.add("查询经期");
                }
            } catch (Exception e) {
                reply = "获取经期信息失败，请稍后再试~";
            }
        }

        AgentChatResponse resp = new AgentChatResponse();
        resp.setReply(reply);
        resp.setEmotion("中立");
        resp.setExecutedActions(actions);
        resp.setNeedFollowUp(false);
        return resp;
    }
}
