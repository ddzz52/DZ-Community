package com.dz.couple.module.agent.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.module.agent.entity.AgentCoreMemory;
import com.dz.couple.module.agent.entity.AgentSceneMemory;
import com.dz.couple.module.agent.memory.MemoryManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记忆管理 API
 * 提供对 Agent 核心记忆和场景记忆的管理接口
 */
@RestController
@RequestMapping("/api/agent/memory")
public class MemoryController {

    @Autowired
    private MemoryManagerService memoryManager;

    /**
     * 保存核心记忆
     */
    @PostMapping("/core")
    public ApiResponse<Void> saveCoreMemory(@RequestBody Map<String, Object> body) {
        Long coupleId = CurrentUser.getCoupleId();
        String type = (String) body.get("memoryType");
        String key = (String) body.get("memoryKey");
        String value = (String) body.get("memoryValue");
        Integer importance = (Integer) body.getOrDefault("importance", 3);
        String source = (String) body.getOrDefault("source", "USER_INPUT");

        memoryManager.saveCoreMemory(coupleId, type, key, value, importance, source);
        return ApiResponse.ok(null);
    }

    /**
     * 查询核心记忆
     */
    @GetMapping("/core")
    public ApiResponse<Map<String, Object>> getCoreMemory(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String key) {
        Long coupleId = CurrentUser.getCoupleId();

        Map<String, Object> result = new HashMap<>();
        if (type != null && key != null) {
            AgentCoreMemory memory = memoryManager.getCoreMemory(coupleId, type, key);
            result.put("memory", memory);
        } else if (type != null) {
            result.put("memories", memoryManager.getCoreMemoriesByType(coupleId, type));
        } else {
            result.put("memories", memoryManager.getAllCoreMemories(coupleId, 50));
        }
        return ApiResponse.ok(result);
    }

    /**
     * 保存场景记忆
     */
    @PostMapping("/scene")
    public ApiResponse<Void> saveSceneMemory(@RequestBody Map<String, Object> body) {
        Long coupleId = CurrentUser.getCoupleId();
        Long userId = CurrentUser.getUserId();
        String type = (String) body.get("memoryType");
        String content = (String) body.get("content");
        String rawData = (String) body.get("rawData");
        String relatedIntent = (String) body.get("relatedIntent");
        Integer importance = (Integer) body.getOrDefault("importance", 3);

        memoryManager.saveSceneMemory(coupleId, userId, type, content, rawData, relatedIntent, importance);
        return ApiResponse.ok(null);
    }

    /**
     * 查询场景记忆
     */
    @GetMapping("/scene")
    public ApiResponse<List<AgentSceneMemory>> getSceneMemories(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "20") Integer limit) {
        Long coupleId = CurrentUser.getCoupleId();

        List<AgentSceneMemory> memories;
        if (type != null) {
            memories = memoryManager.getValidSceneMemoriesByType(coupleId, type);
        } else {
            memories = memoryManager.getAllValidSceneMemories(coupleId, limit);
        }
        return ApiResponse.ok(memories);
    }

    /**
     * 获取全部记忆上下文（用于构建 Prompt）
     */
    @GetMapping("/context")
    public ApiResponse<String> getMemoryContext() {
        Long coupleId = CurrentUser.getCoupleId();
        String context = memoryManager.buildMemoryContext(coupleId);
        return ApiResponse.ok(context);
    }

    /**
     * 清理过期场景记忆
     */
    @PostMapping("/clean-expired")
    public ApiResponse<Map<String, Object>> cleanExpiredMemories() {
        Long coupleId = CurrentUser.getCoupleId();
        int deleted = memoryManager.cleanExpiredSceneMemories(coupleId);

        Map<String, Object> result = new HashMap<>();
        result.put("deleted", deleted);
        return ApiResponse.ok(result);
    }

    /**
     * 删除指定核心记忆
     */
    @DeleteMapping("/core/{id}")
    public ApiResponse<Void> deleteCoreMemory(@PathVariable Long id) {
        memoryManager.deleteCoreMemory(id);
        return ApiResponse.ok(null);
    }
}
