package com.dz.couple.module.agent.intent.handler;

import com.dz.couple.module.agent.dto.AgentChatResponse;
import com.dz.couple.module.agent.intent.IntentEnum;
import com.dz.couple.module.dashboard.dto.DashboardResponse;
import com.dz.couple.module.dashboard.service.DashboardService;
import com.dz.couple.module.timeline.service.TimelineService;
import com.dz.couple.module.user.dto.UserVO;
import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.UserMapper;
import com.dz.couple.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class CoupleLifeIntentHandler implements IntentHandler {

    private static final Set<IntentEnum> SUPPORTED = EnumSet.of(
            IntentEnum.QUERY_COUPLE_INFO,
            IntentEnum.QUERY_TOGETHER_DAYS,
            IntentEnum.UPDATE_LOVE_DATE,
            IntentEnum.QUERY_PROFILE,
            IntentEnum.UPDATE_PROFILE,
            IntentEnum.QUERY_DASHBOARD,
            IntentEnum.QUERY_TIMELINE
    );

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private TimelineService timelineService;

    @Override
    public Set<IntentEnum> supportedIntents() {
        return SUPPORTED;
    }

    @Override
    public AgentChatResponse handle(Long userId, Long coupleId, String message, Map<String, Object> params) {
        List<String> actions = new ArrayList<>();
        String reply;
        String emotion = "开心";

        if (message.contains("多少天") || message.contains("在一起") || message.contains("恋爱天")) {
            // 在一起天数
            UserVO user = userService.getById(userId);
            LocalDate loveDate = user.getLoveDate() != null
                    ? user.getLoveDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    : null;

            if (loveDate == null) {
                reply = "还没有设置恋爱日期哦~ 去「我的」页面设置一下吧！💕";
            } else {
                Period period = Period.between(loveDate, LocalDate.now());
                long days = ChronoUnit.DAYS.between(loveDate, LocalDate.now());
                reply = String.format("💕 你们已经在一起 %d 天啦！\n\n%d 年 %d 个月 %d 天\n\n在一起的每一天都值得珍惜~ ✨",
                        days, period.getYears(), period.getMonths(), period.getDays());
            }
            actions.add("查询恋爱天数");

        } else if (message.contains("首页") || message.contains("概览") || message.contains("dashboard")) {
            // 首页概览
            try {
                DashboardResponse dashboard = dashboardService.getDashboard(userId, coupleId);
                StringBuilder sb = new StringBuilder("🏠 今日概览：\n\n");
                if (dashboard.getAnniversary() != null) {
                    sb.append("📅 最近纪念日：").append(dashboard.getAnniversary().getTitle())
                            .append(" (还有").append(dashboard.getAnniversary().getDaysLeft()).append("天)\n");
                }
                if (dashboard.getBadges() != null) {
                    sb.append("📬 未读消息：").append(dashboard.getBadges().getUnreadMessages()).append("\n");
                    sb.append("🔔 未读通知：").append(dashboard.getBadges().getUnreadNotifications()).append("\n");
                }
                reply = sb.toString();
                actions.add("查看首页");
            } catch (Exception e) {
                reply = "获取首页信息失败，请稍后再试~";
            }

        } else if (message.contains("时间线") || message.contains("回忆") || message.contains("timeline")) {
            reply = "时间线记录了你们的美好回忆~ 去时间线页面滑动看看吧！📜💕";
            actions.add("查看时间线");

        } else if (message.contains("资料") || message.contains("信息") || message.contains("profile")
                || message.contains("昵称") || message.contains("签名")) {
            if (message.contains("修改") || message.contains("改") || message.contains("更新")) {
                reply = "修改个人资料请到「我的」页面操作哦~ 那里可以修改昵称、签名和头像！✨";
                actions.add("引导修改资料");
            } else {
                UserVO me = userService.getById(userId);
                List<User> users = userMapper.listByCoupleId(coupleId);
                StringBuilder sb = new StringBuilder("👤 情侣信息：\n\n");
                for (User u : users) {
                    if (u == null) continue;
                    String nickname = u.getNickname() != null ? u.getNickname() : "未设置";
                    String gender = u.getGender() != null ? (u.getGender() == 1 ? "男" : "女") : "未知";
                    sb.append("  ").append(nickname).append(" (").append(gender).append(")\n");
                }
                reply = sb.toString();
                actions.add("查询信息");
            }

        } else {
            // 情侣信息
            List<User> users = userMapper.listByCoupleId(coupleId);
            StringBuilder sb = new StringBuilder("💑 情侣信息：\n\n");
            for (User u : users) {
                if (u == null) continue;
                sb.append("  ").append(u.getNickname() != null ? u.getNickname() : "对方").append("\n");
            }
            UserVO me = userService.getById(userId);
            if (me.getLoveDate() != null) {
                LocalDate loveDate = me.getLoveDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                long days = ChronoUnit.DAYS.between(loveDate, LocalDate.now());
                sb.append("\n在一起 ").append(days).append(" 天 💕");
            }
            reply = sb.toString();
            actions.add("查询情侣信息");
            emotion = "开心";
        }

        AgentChatResponse resp = new AgentChatResponse();
        resp.setReply(reply);
        resp.setEmotion(emotion);
        resp.setExecutedActions(actions);
        resp.setNeedFollowUp(false);
        return resp;
    }
}
