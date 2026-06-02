package com.dz.couple.module.agent.intent.handler;

import com.dz.couple.module.agent.dto.AgentChatResponse;
import com.dz.couple.module.agent.intent.IntentEnum;
import com.dz.couple.module.anniversary.dto.AnniversaryCreateRequest;
import com.dz.couple.module.anniversary.dto.AnniversaryVO;
import com.dz.couple.module.anniversary.service.AnniversaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class AnniversaryIntentHandler implements IntentHandler {

    private static final Set<IntentEnum> SUPPORTED = EnumSet.of(
            IntentEnum.QUERY_ANNIVERSARY,
            IntentEnum.CREATE_ANNIVERSARY,
            IntentEnum.UPCOMING_ANNIVERSARY,
            IntentEnum.DELETE_ANNIVERSARY
    );

    @Autowired
    private AnniversaryService anniversaryService;

    @Override
    public Set<IntentEnum> supportedIntents() {
        return SUPPORTED;
    }

    @Override
    public AgentChatResponse handle(Long userId, Long coupleId, String message, Map<String, Object> params) {
        List<String> actions = new ArrayList<>();
        String reply;

        List<AnniversaryVO> anniversaries = anniversaryService.list(coupleId);

        // 判断是否有创建意图（基于参数或消息）
        String intent = detectSubIntent(message, params);
        switch (intent) {
            case "CREATE": {
                String title = paramStr(params, "title");
                String date = paramStr(params, "date");

                if (title == null || date == null) {
                    // 从消息中提取
                    title = extractTitle(message);
                    date = extractDate(message);
                }

                if (title != null && date != null) {
                    AnniversaryCreateRequest req = new AnniversaryCreateRequest();
                    req.setTitle(title);
                    req.setDate(date);
                    req.setType("纪念日");
                    anniversaryService.create(userId, coupleId, req);
                    reply = "已经帮你记录纪念日啦~ 💝\n\n" + title + " · " + date;
                    actions.add("创建纪念日");
                } else {
                    reply = "好的！告诉我纪念日名称和日期就好~\n例如：「添加纪念日：第一次约会 2024-02-14」";
                }
                break;
            }
            case "DELETE": {
                reply = "删除纪念日请到纪念日页面操作哦~ 或者告诉我你想删除哪一个？";
                break;
            }
            default: {
                // QUERY / UPCOMING
                if (anniversaries.isEmpty()) {
                    reply = "你们还没有设置纪念日哦~ 要不要我帮你们记录一个重要的日子？💕";
                } else {
                    List<AnniversaryVO> upcoming = anniversaries.stream()
                            .filter(a -> a.getDaysLeft() != null && a.getDaysLeft() >= 0)
                            .sorted(Comparator.comparing(AnniversaryVO::getDaysLeft))
                            .limit(5)
                            .collect(Collectors.toList());

                    if (upcoming.isEmpty()) {
                        reply = "近期没有即将到来的纪念日呢~ 去看看已记录的纪念日吧 💝";
                    } else {
                        StringBuilder sb = new StringBuilder("📅 最近的纪念日：\n\n");
                        for (AnniversaryVO ann : upcoming) {
                            String days = ann.getDaysLeft() == 0 ? "🎉 就是今天！" : "还有 " + ann.getDaysLeft() + " 天";
                            sb.append("💝 ").append(ann.getTitle()).append("：").append(days).append("\n");
                        }
                        sb.append("\n").append("共 ").append(anniversaries.size()).append(" 个纪念日");
                        reply = sb.toString();
                    }
                    actions.add("查询纪念日");
                }
            }
        }

        AgentChatResponse resp = new AgentChatResponse();
        resp.setReply(reply);
        resp.setEmotion("开心");
        resp.setExecutedActions(actions);
        resp.setNeedFollowUp(false);
        return resp;
    }

    private String detectSubIntent(String message, Map<String, Object> params) {
        if (message.contains("删除") || message.contains("去掉")) return "DELETE";
        if (message.contains("添加") || message.contains("新增") || message.contains("创建") || message.contains("记录")
                || params.containsKey("title")) return "CREATE";
        return "QUERY";
    }

    private String paramStr(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v != null ? v.toString() : null;
    }

    private String extractDate(String message) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(message);
        return m.find() ? m.group(1) : null;
    }

    private String extractTitle(String message) {
        String t = message.replaceAll("(添加|新增|创建|记录)\\s*纪念日[:：]?", "");
        t = t.replaceAll("\\d{4}-\\d{2}-\\d{2}", "");
        t = t.replaceAll("\\s+", " ").trim();
        return t.isEmpty() ? null : (t.length() > 64 ? t.substring(0, 64) : t);
    }
}
