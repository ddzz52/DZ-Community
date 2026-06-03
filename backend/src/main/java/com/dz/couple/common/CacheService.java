package com.dz.couple.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 通用 Redis 缓存服务
 * Redis 不可用时自动降级（静默跳过缓存，直接走 DB）
 */
@Service
public class CacheService {
    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired(required = false)
    private StringRedisTemplate redis;

    /** 读缓存，未命中返回 null */
    public <T> T get(String key, Class<T> clazz) {
        if (redis == null) return null;
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? null : JSON.readValue(json, clazz);
        } catch (Exception e) { return null; }
    }

    /** 写缓存 */
    public void set(String key, Object value, long ttlSeconds) {
        if (redis == null || value == null) return;
        try {
            String json = JSON.writeValueAsString(value);
            redis.opsForValue().set(key, json, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) { log.debug("Cache write failed: {}", key); }
    }

    /** 删除缓存 */
    public void delete(String key) {
        if (redis == null) return;
        try { redis.delete(key); } catch (Exception e) { /* ignore */ }
    }

    /** 批量删除匹配的 key */
    public void deletePattern(String pattern) {
        if (redis == null) return;
        try {
            Set<String> keys = redis.keys(pattern);
            if (keys != null && !keys.isEmpty()) redis.delete(keys);
        } catch (Exception e) { /* ignore */ }
    }
}
