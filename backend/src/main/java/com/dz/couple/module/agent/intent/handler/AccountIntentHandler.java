package com.dz.couple.module.agent.intent.handler;

import com.dz.couple.module.account.dto.*;
import com.dz.couple.module.account.service.AccountService;
import com.dz.couple.module.agent.dto.AgentChatResponse;
import com.dz.couple.module.agent.intent.IntentEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class AccountIntentHandler implements IntentHandler {

    private static final Set<IntentEnum> SUPPORTED = EnumSet.of(
            IntentEnum.ADD_ACCOUNT,
            IntentEnum.QUERY_ACCOUNT,
            IntentEnum.ACCOUNT_STATS
    );

    @Autowired
    private AccountService accountService;

    @Override
    public Set<IntentEnum> supportedIntents() {
        return SUPPORTED;
    }

    @Override
    public AgentChatResponse handle(Long userId, Long coupleId, String message, Map<String, Object> params) {
        List<String> actions = new ArrayList<>();
        String reply;

        if (message.contains("统计") || message.contains("汇总") || message.contains("分析")
                || message.contains("月") || message.contains("占比")) {
            // 记账统计
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;

            try {
                AccountMonthStatsResponse stats = accountService.monthStats(userId, coupleId, year, month);
                BigDecimal total = stats.getTotalAmount() != null ? stats.getTotalAmount() : BigDecimal.ZERO;

                StringBuilder sb = new StringBuilder();
                sb.append("💰 ").append(year).append("年").append(month).append("月 账单统计：\n\n");
                sb.append("总支出：¥").append(total.setScale(2, BigDecimal.ROUND_HALF_UP)).append("\n\n");

                List<AccountMonthStatsResponse.CategoryItem> byCategory = stats.getByCategory();
                if (byCategory != null && !byCategory.isEmpty()) {
                    sb.append("分类统计：\n");
                    java.util.Collections.sort(byCategory,
                            (a, b) -> b.getAmount().compareTo(a.getAmount()));
                    int limit = Math.min(5, byCategory.size());
                    for (int i = 0; i < limit; i++) {
                        AccountMonthStatsResponse.CategoryItem item = byCategory.get(i);
                        sb.append("  ").append(item.getCategory()).append(": ¥")
                                .append(item.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP)).append("\n");
                    }
                }
                reply = sb.toString();
                actions.add("记账统计");

            } catch (Exception e) {
                reply = "获取账单统计失败，请稍后再试~";
            }

        } else if (message.contains("花了") || message.contains("消费") || message.contains("买")
                || message.contains("账单") || message.contains("支出") || message.contains("报销")
                || params.containsKey("amount")) {
            // 添加记账
            BigDecimal amount = parseAmount(params, message);
            String category = paramStr(params, "category");
            if (category == null) category = detectCategory(message);
            String remark = paramStr(params, "remark");
            if (remark == null) remark = extractRemark(message);
            String date = paramStr(params, "date");

            if (amount == null) {
                reply = "好的，告诉我花了多少钱和用在什么地方就好~\n例如：「午餐花了 35 块」或「记账 200 买礼物」💰";
            } else {
                AccountCreateRequest req = new AccountCreateRequest();
                req.setAmount(amount);
                req.setCategory(category);
                req.setRemark(remark);
                if (date != null) req.setOccurredAt(date);

                try {
                    accountService.create(userId, coupleId, req);
                    reply = "已记账~ 💰\n\n" + category + " ¥" + amount.setScale(2, BigDecimal.ROUND_HALF_UP);
                    if (remark != null) reply += "\n备注：" + remark;
                    actions.add("添加记账");
                } catch (Exception e) {
                    reply = "记账失败：" + e.getMessage();
                }
            }

        } else {
            // 查询记账
            List<AccountVO> list = accountService.list(userId, coupleId, null, null, null, null, null, 5);
            if (list.isEmpty()) {
                reply = "还没有记账记录呢~ 告诉我花了多少钱，我帮你记！💳";
            } else {
                StringBuilder sb = new StringBuilder("💰 最近账单：\n\n");
                SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
                BigDecimal sum = BigDecimal.ZERO;
                for (int i = 0; i < Math.min(5, list.size()); i++) {
                    AccountVO a = list.get(i);
                    sum = sum.add(a.getAmount() != null ? a.getAmount() : BigDecimal.ZERO);
                    String dateStr = a.getOccurredAt() != null ? sdf.format(a.getOccurredAt()) : "";
                    sb.append("  ").append(dateStr).append(" ")
                            .append(a.getCategory()).append(" ¥")
                            .append(a.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP)).append("\n");
                }
                sb.append("\n最近5笔合计: ¥").append(sum.setScale(2, BigDecimal.ROUND_HALF_UP));
                reply = sb.toString();
            }
            actions.add("查询记账");
        }

        AgentChatResponse resp = new AgentChatResponse();
        resp.setReply(reply);
        resp.setEmotion("中立");
        resp.setExecutedActions(actions);
        resp.setNeedFollowUp(false);
        return resp;
    }

    private BigDecimal parseAmount(Map<String, Object> params, String message) {
        Object amt = params.get("amount");
        if (amt instanceof Number) {
            return BigDecimal.valueOf(((Number) amt).doubleValue());
        }
        // 从消息中提取：匹配 "数字+元/块"
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+(\\.\\d{1,2})?)\\s*[元块钱]").matcher(message);
        if (m.find()) {
            return new BigDecimal(m.group(1));
        }
        return null;
    }

    private String detectCategory(String message) {
        if (containsAny(message, "吃", "饭", "餐", "外卖", "奶茶", "咖啡")) return "餐饮";
        if (containsAny(message, "车", "地铁", "公交", "打车", "加油", "出行")) return "交通";
        if (containsAny(message, "买", "购", "淘宝", "京东", "衣服", "鞋")) return "购物";
        if (containsAny(message, "电影", "游戏", "玩", "KTV", "旅游", "门票")) return "娱乐";
        if (containsAny(message, "房租", "水电", "物业", "租金")) return "居住";
        if (containsAny(message, "电话", "网费", "充值")) return "通讯";
        return "其他";
    }

    private String extractRemark(String message) {
        String r = message.replaceAll("\\d+(\\.\\d{1,2})?\\s*[元块钱]", "");
        r = r.replaceAll("(记账|记录|花了|消费|支出|帮|我|添加|新增)", "").trim();
        return r.isEmpty() ? null : (r.length() > 50 ? r.substring(0, 50) : r);
    }

    private String paramStr(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v != null ? v.toString() : null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
