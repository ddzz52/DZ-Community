package com.dz.couple.module.agent.intent.handler;

import com.dz.couple.module.agent.dto.AgentChatResponse;
import com.dz.couple.module.agent.intent.IntentEnum;
import com.dz.couple.module.memo.dto.MemoCategoryVO;
import com.dz.couple.module.memo.dto.MemoCreateRequest;
import com.dz.couple.module.memo.dto.MemoVO;
import com.dz.couple.module.memo.service.MemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MemoIntentHandler implements IntentHandler {

    private static final Set<IntentEnum> SUPPORTED = EnumSet.of(
            IntentEnum.CREATE_MEMO,
            IntentEnum.QUERY_MEMO,
            IntentEnum.COMPLETE_MEMO
    );

    @Autowired
    private MemoService memoService;

    @Override
    public Set<IntentEnum> supportedIntents() {
        return SUPPORTED;
    }

    @Override
    public AgentChatResponse handle(Long userId, Long coupleId, String message, Map<String, Object> params) {
        List<String> actions = new ArrayList<>();
        String reply;

        if (message.contains("创建") || message.contains("添加") || message.contains("记一下")
                || message.contains("提醒我") || message.contains("记得") || params.containsKey("title")) {
            // 创建备忘录
            String title = paramStr(params, "title");
            if (title == null) title = "AI 提醒";
            String content = paramStr(params, "content");
            if (content == null) {
                content = message.replaceAll("(添加|新增|创建|记一下|提醒我|记得|帮|我|备忘录)", "").trim();
                if (content.isEmpty()) content = message;
            }

            MemoCreateRequest req = new MemoCreateRequest();
            req.setTitle(title);
            req.setContent(content);
            req.setStatus(0);
            req.setCategoryId(pickDefaultCategory(userId, coupleId));

            try {
                memoService.createMemo(userId, coupleId, req);
                reply = "备忘录已经创建好啦~ 📌\n\n「" + title + "」\n" + preview(content, 30) + "\n\n我会记得提醒你们的！💪";
                actions.add("创建备忘录");
            } catch (Exception e) {
                reply = "备忘录创建失败了，请稍后再试~ 🥺";
            }

        } else if (message.contains("完成") || message.contains("做完了") || message.contains("搞定")) {
            reply = "太棒了！请到备忘录页面把它标记为完成吧~ ✅\n完成的事情值得被记录下来！💪";
            actions.add("标记完成");

        } else {
            // 查询备忘录
            List<MemoCategoryVO> categories = memoService.listCategories(userId, coupleId);
            StringBuilder sb = new StringBuilder("📌 备忘录分类：\n\n");

            if (categories != null && !categories.isEmpty()) {
                for (MemoCategoryVO cat : categories) {
                    if (cat == null || cat.getName() == null) continue;
                    sb.append("  📁 ").append(cat.getName()).append("\n");
                }
                sb.append("\n去备忘录页面查看详情吧~");
                reply = sb.toString();
            } else {
                reply = "还没有备忘录呢~ 告诉我需要记住什么，我帮你记下来！📝";
            }
            actions.add("查询备忘录");
        }

        AgentChatResponse resp = new AgentChatResponse();
        resp.setReply(reply);
        resp.setEmotion("开心");
        resp.setExecutedActions(actions);
        resp.setNeedFollowUp(false);
        return resp;
    }

    private Long pickDefaultCategory(Long userId, Long coupleId) {
        List<MemoCategoryVO> cats = memoService.listCategories(userId, coupleId);
        if (cats == null || cats.isEmpty()) return null;
        for (MemoCategoryVO c : cats) {
            if (c != null && c.getName() != null && c.getName().contains("日常")) return c.getId();
        }
        MemoCategoryVO first = cats.get(0);
        return first != null ? first.getId() : null;
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
}
