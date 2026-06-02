package com.dz.couple.module.agent.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.agent.dto.AgentChatRequest;
import com.dz.couple.module.agent.dto.AgentChatResponse;
import com.dz.couple.module.agent.entity.AgentInteraction;
import com.dz.couple.module.agent.mapper.AgentInteractionMapper;
import com.dz.couple.module.agent.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@Validated
public class AgentController {

    @Autowired
    private AgentService agentService;

    @Autowired
    private AgentInteractionMapper interactionMapper;

    /** 查询历史对话（分页） */
    @GetMapping("/history")
    public ApiResponse<Map<String, Object>> history(@RequestParam(value = "beforeId", required = false) Long beforeId,
                                                     @RequestParam(value = "limit", required = false, defaultValue = "30") int limit) {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        int l = Math.max(1, Math.min(limit, 100));
        List<AgentInteraction> list = interactionMapper.findHistory(userId, beforeId, l);
        if (list == null) list = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AgentInteraction it : list) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", it.getId());
            row.put("type", "agent".equals(it.getInteractionType()) ? "agent" : "user");
            row.put("content", "user".equals(it.getInteractionType()) ? it.getUserMessage() : it.getAgentResponse());
            row.put("intent", it.getIntent());
            row.put("time", it.getCreatedAt());
            rows.add(row);
        }
        boolean hasMore = list.size() >= l;
        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("hasMore", hasMore);
        return ApiResponse.ok(result);
    }

    @PostMapping("/chat")
    public ApiResponse<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(agentService.chat(userId, coupleId, request));
    }

    @PostMapping("/proactive-care")
    public ApiResponse<Void> triggerProactiveCare() {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        agentService.proactiveCare(coupleId);
        return ApiResponse.ok(null);
    }

    /** 删除单条对话记录 */
    @DeleteMapping("/messages/{id}")
    public ApiResponse<Void> deleteMessage(@PathVariable Long id) {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        int rows = interactionMapper.deleteById(id, userId);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return ApiResponse.ok(null);
    }

    /** 批量删除对话记录 */
    @PostMapping("/messages/batch-delete")
    public ApiResponse<Integer> batchDeleteMessages(@RequestBody List<Long> ids) {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.ok(0);
        }
        int rows = interactionMapper.deleteBatch(ids, userId);
        return ApiResponse.ok(rows);
    }

    /** 清空当前用户所有对话记录 */
    @DeleteMapping("/messages")
    public ApiResponse<Integer> clearMessages() {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        int rows = interactionMapper.deleteAllByUser(userId);
        return ApiResponse.ok(rows);
    }
}
