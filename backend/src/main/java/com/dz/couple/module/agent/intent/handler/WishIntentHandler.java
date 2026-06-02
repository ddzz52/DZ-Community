package com.dz.couple.module.agent.intent.handler;

import com.dz.couple.module.agent.dto.AgentChatResponse;
import com.dz.couple.module.agent.intent.IntentEnum;
import com.dz.couple.module.wish.dto.WishScratchCreateRequest;
import com.dz.couple.module.wish.dto.WishScratchVO;
import com.dz.couple.module.wish.service.WishScratchService;
import com.dz.couple.module.wishlist.dto.WishItemCreateRequest;
import com.dz.couple.module.wishlist.dto.WishItemVO;
import com.dz.couple.module.wishlist.service.WishListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class WishIntentHandler implements IntentHandler {

    private static final Set<IntentEnum> SUPPORTED = EnumSet.of(
            IntentEnum.CREATE_WISH_SCRATCH,
            IntentEnum.QUERY_WISH_SCRATCH,
            IntentEnum.ADD_WISH,
            IntentEnum.QUERY_WISH,
            IntentEnum.SPIN_WISH
    );

    @Autowired
    private WishScratchService wishScratchService;

    @Autowired
    private WishListService wishListService;

    @Override
    public Set<IntentEnum> supportedIntents() {
        return SUPPORTED;
    }

    @Override
    public AgentChatResponse handle(Long userId, Long coupleId, String message, Map<String, Object> params) {
        List<String> actions = new ArrayList<>();
        String reply;

        if (message.contains("刮刮乐") || message.contains("刮开") || message.contains("刮卡")) {
            // 刮刮乐相关
            if (message.contains("创建") || message.contains("制作") || message.contains("做") || params.containsKey("content")) {
                String content = paramStr(params, "content");
                if (content == null) {
                    content = message.replaceAll("(创建|制作|做|刮刮乐|帮|我|添加)", "").trim();
                }
                if (content == null || content.isEmpty()) {
                    reply = "想做什么样的刮刮乐呢？告诉我内容就好~ 例如：「做个刮刮乐：一顿大餐」🎁";
                } else if (content.length() > 50) {
                    reply = "刮刮乐内容不能超过50个字哦~ 精简一下？";
                } else {
                    try {
                        WishScratchCreateRequest req = new WishScratchCreateRequest();
                        req.setContent(content);
                        req.setRevealMode(0);
                        WishScratchVO vo = wishScratchService.create(userId, coupleId, req);
                        reply = "刮刮乐已经做好啦~ 🎁✨\n\n「" + content + "」\n快去刮开看看吧！";
                        actions.add("创建刮刮乐");
                    } catch (Exception e) {
                        reply = "创建刮刮乐失败：" + e.getMessage();
                    }
                }
            } else {
                List<WishScratchVO> list = wishScratchService.list(coupleId, null);
                if (list.isEmpty()) {
                    reply = "还没有刮刮乐呢~ 要不要做一个给TA惊喜？🎁";
                } else {
                    StringBuilder sb = new StringBuilder("🎁 你们的刮刮乐：\n\n");
                    int unScratched = 0;
                    for (WishScratchVO vo : list) {
                        String status = vo.getStatus() != null && vo.getStatus() == 1 ? "已刮开" : "未刮开";
                        if (vo.getStatus() == null || vo.getStatus() == 0) unScratched++;
                        sb.append("  ").append(status).append("  ").append(vo.getContent()).append("\n");
                    }
                    sb.append("\n").append(unScratched).append(" 张等待刮开~");
                    reply = sb.toString();
                }
                actions.add("查询刮刮乐");
            }

        } else {
            // 心愿清单相关
            if (message.contains("转盘") || message.contains("抽") || message.contains("随机") || message.contains("roulette")) {
                try {
                    WishItemVO picked = wishListService.roulette(userId, coupleId);
                    reply = "🎯 转盘抽中了：\n\n「" + picked.getContent() + "」\n\n优先级：" + priorityText(picked.getPriority())
                            + "\n期望日期：" + picked.getExpectedAt() + "\n\n快去实现它吧！💪";
                    actions.add("转盘抽心愿");
                } catch (Exception e) {
                    reply = "抽心愿失败：" + e.getMessage();
                }
            } else if (message.contains("添加") || message.contains("新增") || message.contains("加") || params.containsKey("content")) {
                String content = paramStr(params, "content");
                if (content == null) {
                    content = message.replaceAll("(添加|新增|加|心愿|帮|我|记录)", "").trim();
                }
                if (content == null || content.isEmpty()) {
                    reply = "你想添加什么心愿呢？告诉我内容和期望时间~ 例如：「添加心愿：一起去海边 2026-08-01」✨";
                } else {
                    String expectedDate = paramStr(params, "expectedDate");
                    if (expectedDate == null) expectedDate = "2099-12-31";
                    String priority = paramStr(params, "priority");
                    int p = "高".equals(priority) ? WishListService.PRIORITY_HIGH
                            : "低".equals(priority) ? WishListService.PRIORITY_LOW : WishListService.PRIORITY_MEDIUM;

                    try {
                        WishItemCreateRequest req = new WishItemCreateRequest();
                        req.setContent(content);
                        req.setExpectedAt(expectedDate);
                        req.setPriority(p);
                        WishItemVO vo = wishListService.create(userId, coupleId, req);
                        reply = "心愿已添加~ ✨\n\n「" + content + "」\n期望日期：" + expectedDate + "\n\n一起努力实现它吧！💕";
                        actions.add("添加心愿");
                    } catch (Exception e) {
                        reply = "添加心愿失败：" + e.getMessage();
                    }
                }
            } else {
                List<WishItemVO> list = wishListService.list(coupleId, null);
                if (list.isEmpty()) {
                    reply = "还没有心愿清单呢~ 告诉我你们的愿望，一起记下来吧！✨";
                } else {
                    StringBuilder sb = new StringBuilder("✨ 心愿清单：\n\n");
                    int pending = 0;
                    for (WishItemVO vo : list) {
                        if (vo.getStatus() != null && vo.getStatus() == 0) pending++;
                        String mark = vo.getStatus() != null && vo.getStatus() == 1 ? "✅" : "⭐";
                        sb.append(mark).append(" ").append(vo.getContent()).append("\n");
                    }
                    sb.append("\n").append(pending).append(" 个待实现的心愿~");
                    reply = sb.toString();
                }
                actions.add("查询心愿");
            }
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

    private String priorityText(Integer p) {
        if (p == null) return "中";
        if (p == WishListService.PRIORITY_HIGH) return "高";
        if (p == WishListService.PRIORITY_LOW) return "低";
        return "中";
    }
}
