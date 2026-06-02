package com.dz.couple.module.agent.schedule;

import com.dz.couple.module.agent.service.AgentService;
import com.dz.couple.module.couple.mapper.CoupleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentProactiveScheduler {
    private static final Logger log = LoggerFactory.getLogger(AgentProactiveScheduler.class);

    @Autowired
    private AgentService agentService;

    @Autowired
    private CoupleMapper coupleMapper;

    @Scheduled(cron = "0 */30 * * * *")
    public void runProactiveCare() {
        List<Long> coupleIds = coupleMapper.listAllIds();
        if (coupleIds == null || coupleIds.isEmpty()) {
            return;
        }
        for (Long coupleId : coupleIds) {
            if (coupleId == null) {
                continue;
            }
            try {
                agentService.proactiveCare(coupleId);
            } catch (Exception e) {
                log.warn("主动关怀执行失败: coupleId={}", coupleId, e);
            }
        }
    }
}
