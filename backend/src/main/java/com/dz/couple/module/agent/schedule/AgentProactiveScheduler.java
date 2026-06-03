package com.dz.couple.module.agent.schedule;

import com.dz.couple.module.agent.service.AgentService;
import com.dz.couple.module.couple.mapper.CoupleMapper;
import com.dz.couple.module.period.service.PeriodService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时任务调度
 * 原 Dashboard 每次请求时触发的 DB 写操作（纪念日/经期提醒生成）已全部移除此处
 */
@Component
public class AgentProactiveScheduler {
    private static final Logger log = LoggerFactory.getLogger(AgentProactiveScheduler.class);

    @Autowired
    private AgentService agentService;

    @Autowired
    private CoupleMapper coupleMapper;

    @Autowired
    private PeriodService periodService;

    /** 每30分钟：纪念日/经期提醒生成 + AI主动关怀 */
    @Scheduled(cron = "0 */30 * * * *")
    public void runProactiveCare() {
        List<Long> coupleIds = coupleMapper.listAllIds();
        if (coupleIds == null || coupleIds.isEmpty()) return;
        for (Long coupleId : coupleIds) {
            if (coupleId == null) continue;
            try {
                // 经期提醒（原在 Dashboard 每次请求时生成）
                try { periodService.ensurePeriodReminders(coupleId); } catch (Exception e) {
                    log.warn("经期提醒生成失败 coupleId={}: {}", coupleId, e.getMessage());
                }
                // AI 主动关怀（内含纪念日提醒生成）
                agentService.proactiveCare(coupleId);
            } catch (Exception e) {
                log.warn("定时任务执行失败: coupleId={}", coupleId, e);
            }
        }
    }
}
