package com.dz.couple.module.user.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录频率限制 — 基于 Redis
 * 同 IP 5 分钟内失败 5 次 → 锁定 15 分钟
 */
@Service
public class LoginRateLimitService {
    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitService.class);

    private static final String PREFIX = "login:limit:";
    private static final int MAX_FAILURES = 5;
    private static final int FAIL_WINDOW_MINUTES = 5;
    private static final int LOCK_MINUTES = 15;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /** 检查是否被锁定，未锁定则返回 true */
    public void check(String ip) {
        if (redisTemplate == null || ip == null || ip.trim().isEmpty()) return;

        String lockKey = PREFIX + "lock:" + ip.trim();
        String locked = redisTemplate.opsForValue().get(lockKey);
        if (locked != null) {
            Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            long minutes = (ttl != null && ttl > 0) ? ttl / 60 + 1 : LOCK_MINUTES;
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "登录尝试过于频繁，请 " + minutes + " 分钟后再试");
        }
    }

    /** 记录一次失败 */
    public void recordFailure(String ip) {
        if (redisTemplate == null || ip == null || ip.trim().isEmpty()) return;

        String failKey = PREFIX + "fail:" + ip.trim();
        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count == 1) {
            redisTemplate.expire(failKey, FAIL_WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        if (count != null && count >= MAX_FAILURES) {
            String lockKey = PREFIX + "lock:" + ip.trim();
            redisTemplate.opsForValue().set(lockKey, "1", LOCK_MINUTES, TimeUnit.MINUTES);
            redisTemplate.delete(failKey);
            log.warn("IP {} 登录锁定 {} 分钟（{} 次失败）", ip, LOCK_MINUTES, count);
        }
    }

    /** 登录成功后清除失败计数 */
    public void clearOnSuccess(String ip) {
        if (redisTemplate == null || ip == null || ip.trim().isEmpty()) return;
        redisTemplate.delete(PREFIX + "fail:" + ip.trim());
        redisTemplate.delete(PREFIX + "lock:" + ip.trim());
    }
}
