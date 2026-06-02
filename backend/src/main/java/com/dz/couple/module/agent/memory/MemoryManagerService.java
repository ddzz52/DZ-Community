package com.dz.couple.module.agent.memory;

import com.dz.couple.module.agent.entity.AgentCoreMemory;
import com.dz.couple.module.agent.entity.AgentSceneMemory;
import com.dz.couple.module.agent.mapper.AgentCoreMemoryMapper;
import com.dz.couple.module.agent.mapper.AgentSceneMemoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记忆管理服务层
 * 负责核心记忆和场景记忆的 CRUD 操作
 */
@Service
public class MemoryManagerService {

    private static final Logger log = LoggerFactory.getLogger(MemoryManagerService.class);

    @Autowired
    private AgentCoreMemoryMapper coreMemoryMapper;

    @Autowired
    private AgentSceneMemoryMapper sceneMemoryMapper;

    // ==================== 核心记忆操作 ====================

    /**
     * 保存/更新核心记忆
     */
    public void saveCoreMemory(Long coupleId, String type, String key, String value, int importance, String source) {
        if (coupleId == null || type == null || key == null || value == null) {
            return;
        }
        coreMemoryMapper.upsert(coupleId, type, key, value, importance, source);
        log.info("核心记忆已保存：coupleId={}, type={}, key={}, source={}", coupleId, type, key, source);
    }

    /**
     * 查询单条核心记忆
     */
    public AgentCoreMemory getCoreMemory(Long coupleId, String type, String key) {
        return coreMemoryMapper.findByCoupleAndKey(coupleId, type, key);
    }

    /**
     * 获取指定类型的全部核心记忆
     */
    public List<AgentCoreMemory> getCoreMemoriesByType(Long coupleId, String type) {
        return coreMemoryMapper.findByCoupleAndType(coupleId, type);
    }

    /**
     * 获取全部核心记忆（按重要度排序）
     */
    public List<AgentCoreMemory> getAllCoreMemories(Long coupleId, int limit) {
        return coreMemoryMapper.listByCoupleId(coupleId, limit);
    }

    /**
     * 获取单条核心记忆的值（快捷方法）
     */
    public String getCoreMemoryValue(Long coupleId, String type, String key) {
        AgentCoreMemory memory = getCoreMemory(coupleId, type, key);
        return memory != null ? memory.getMemoryValue() : null;
    }

    /**
     * 删除核心记忆
     */
    public void deleteCoreMemory(Long id) {
        coreMemoryMapper.deleteById(id);
    }

    // ==================== 场景记忆操作 ====================

    /**
     * 保存场景记忆（自动设置过期时间）
     */
    public void saveSceneMemory(Long coupleId, Long userId, String type, String content,
                                String rawData, String relatedIntent, int importance) {
        if (coupleId == null) {
            return;
        }
        AgentSceneMemory memory = new AgentSceneMemory();
        memory.setCoupleId(coupleId);
        memory.setUserId(userId);
        memory.setMemoryType(type);
        memory.setContent(content);
        memory.setRawData(rawData);
        memory.setRelatedIntent(relatedIntent);
        memory.setImportance(importance);
        memory.setExpireAt(LocalDateTime.now().plusDays(30));

        sceneMemoryMapper.insert(memory);
        log.info("场景记忆已保存：coupleId={}, userId={}, type={}, relatedIntent={}",
                coupleId, userId, type, relatedIntent);
    }

    /**
     * 查询有效的场景记忆（按类型）
     */
    public List<AgentSceneMemory> getValidSceneMemoriesByType(Long coupleId, String type) {
        return sceneMemoryMapper.findValidByCoupleAndType(coupleId, type);
    }

    /**
     * 获取全部有效的场景记忆
     */
    public List<AgentSceneMemory> getAllValidSceneMemories(Long coupleId, int limit) {
        return sceneMemoryMapper.listValidByCoupleId(coupleId, limit);
    }

    /**
     * 删除过期场景记忆
     */
    public int cleanExpiredSceneMemories(Long coupleId) {
        int deleted = sceneMemoryMapper.deleteExpired(coupleId, LocalDateTime.now());
        if (deleted > 0) {
            log.info("清理过期场景记忆：coupleId={}, count={}", coupleId, deleted);
        }
        return deleted;
    }

    /**
     * 删除所有旧的场景记忆（超过30天）
     */
    public int cleanOldSceneMemories() {
        LocalDateTime before = LocalDateTime.now().minusDays(30);
        return sceneMemoryMapper.deleteOlderThan(before);
    }

    // ==================== 记忆合并输出 ====================

    /**
     * 获取 Agent 可用的全部记忆（用于构建 Prompt）
     * 包含核心记忆 + 近期场景记忆
     */
    public String buildMemoryContext(Long coupleId) {
        StringBuilder sb = new StringBuilder();

        // 1. 核心记忆
        List<AgentCoreMemory> coreMemories = getAllCoreMemories(coupleId, 50);
        if (!coreMemories.isEmpty()) {
            sb.append("【情侣信息】\n");
            for (AgentCoreMemory m : coreMemories) {
                sb.append("- ").append(m.getMemoryKey()).append(": ").append(m.getMemoryValue()).append("\n");
            }
            sb.append("\n");
        }

        // 2. 近期场景记忆
        List<AgentSceneMemory> sceneMemories = getAllValidSceneMemories(coupleId, 20);
        if (!sceneMemories.isEmpty()) {
            sb.append("【近期记忆】\n");
            for (AgentSceneMemory m : sceneMemories) {
                sb.append("- [").append(m.getCreatedAt().toLocalDate()).append("] ")
                        .append(m.getContent()).append("\n");
            }
        }

        return sb.toString();
    }
}
