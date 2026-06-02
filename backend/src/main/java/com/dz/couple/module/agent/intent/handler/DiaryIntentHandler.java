package com.dz.couple.module.agent.intent.handler;

import com.dz.couple.module.agent.dto.AgentChatResponse;
import com.dz.couple.module.agent.intent.IntentEnum;
import com.dz.couple.module.diary.dto.DiaryCreateRequest;
import com.dz.couple.module.diary.dto.DiaryVO;
import com.dz.couple.module.diary.service.DiaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class DiaryIntentHandler implements IntentHandler {

    private static final Set<IntentEnum> SUPPORTED = EnumSet.of(
            IntentEnum.CREATE_DIARY,
            IntentEnum.QUERY_DIARY,
            IntentEnum.DIARY_STATS
    );

    @Autowired
    private DiaryService diaryService;

    @Override
    public Set<IntentEnum> supportedIntents() {
        return SUPPORTED;
    }

    @Override
    public AgentChatResponse handle(Long userId, Long coupleId, String message, Map<String, Object> params) {
        List<String> actions = new ArrayList<>();
        String reply;

        if (message.contains("写") || message.contains("记") || message.contains("记录")
                || message.contains("添加") || params.containsKey("content")) {
            // 创建日记
            String content = paramStr(params, "content");
            if (content == null) {
                content = message.replaceAll("(帮)?(我)?(写|记|记录|添加)(一篇)?(日记)?", "").trim();
                if (content.isEmpty()) content = message;
            }
            String mood = paramStr(params, "mood");
            if (mood == null) mood = detectMood(message);

            DiaryCreateRequest req = new DiaryCreateRequest();
            req.setContent(content);
            req.setMood(mood);
            req.setPrivateFlag(params.containsKey("private") && Boolean.TRUE.equals(params.get("private")));

            try {
                diaryService.create(userId, coupleId, req);
                reply = "已经帮你写进日记啦~ 📝✨\n\n心情：" + mood + "\n内容预览：" + preview(content, 50) + "\n\n今天的故事也被好好记录下来了~ 💕";
                actions.add("创建日记");
            } catch (Exception e) {
                reply = "日记创建失败了，请稍后再试~ 🥺";
            }

        } else if (message.contains("统计") || message.contains("分析") || message.contains("什么心情")) {
            // 心情统计
            List<DiaryVO> diaries = diaryService.list(userId, coupleId, null, 30);
            Map<String, Long> moodCount = new LinkedHashMap<>();
            for (DiaryVO d : diaries) {
                String m = d.getMood() != null ? d.getMood() : "未知";
                moodCount.put(m, moodCount.getOrDefault(m, 0L) + 1);
            }
            StringBuilder sb = new StringBuilder("📊 最近日记心情统计：\n\n");
            if (moodCount.isEmpty()) {
                sb.append("还没有日记记录，先写一篇吧~");
            } else {
                moodCount.entrySet().stream()
                        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                        .forEach(e -> sb.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append(" 篇\n"));
            }
            reply = sb.toString();
            actions.add("日记统计");

        } else {
            // 查询日记
            List<DiaryVO> diaries = diaryService.list(userId, coupleId, null, 5);
            if (diaries.isEmpty()) {
                reply = "你们还没有写过日记呢~ 要不要一起记录今天的故事？📖";
            } else {
                StringBuilder sb = new StringBuilder("📝 最近的日记：\n\n");
                SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
                for (int i = 0; i < Math.min(3, diaries.size()); i++) {
                    DiaryVO d = diaries.get(i);
                    String date = d.getCreatedAt() != null ? sdf.format(d.getCreatedAt()) : "某天";
                    String mood = d.getMood() != null ? "[" + d.getMood() + "] " : "";
                    sb.append("📝 ").append(date).append(" ").append(mood)
                            .append(preview(d.getContent(), 40)).append("\n\n");
                }
                reply = sb.toString();
            }
            actions.add("查询日记");
        }

        AgentChatResponse resp = new AgentChatResponse();
        resp.setReply(reply);
        resp.setEmotion("开心");
        resp.setExecutedActions(actions);
        resp.setNeedFollowUp(false);
        return resp;
    }

    private String detectMood(String message) {
        if (containsAny(message, "开心", "高兴", "快乐", "幸福", "开心")) return "开心";
        if (containsAny(message, "难过", "伤心", "哭", "委屈")) return "难过";
        if (containsAny(message, "生气", "烦", "怒")) return "生气";
        return "开心";
    }

    private String paramStr(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v != null ? v.toString() : null;
    }

    private String preview(String s, int maxLen) {
        if (s == null) return "";
        String clean = s.replaceAll("\\s+", " ");
        return clean.length() > maxLen ? clean.substring(0, maxLen) + "..." : clean;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
