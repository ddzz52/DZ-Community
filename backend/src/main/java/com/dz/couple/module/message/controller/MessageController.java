package com.dz.couple.module.message.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.message.dto.MessageCreateRequest;
import com.dz.couple.module.message.dto.MessageVO;
import com.dz.couple.module.message.service.MessageService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
@Validated
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ApiResponse<List<MessageVO>> list(@RequestParam(value = "beforeId", required = false) Long beforeId,
                                            @RequestParam(value = "limit", required = false) Integer limit) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(messageService.list(userId, coupleId, beforeId, limit == null ? 50 : limit));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> unreadCount() {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(messageService.countUnread(userId, coupleId));
    }

    @PostMapping
    public ApiResponse<MessageVO> send(@Valid @RequestBody MessageCreateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(messageService.send(userId, coupleId, req));
    }

    @PutMapping("/read-up-to/{id}")
    public ApiResponse<Integer> readUpTo(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(messageService.markReadUpTo(userId, coupleId, id));
    }

    @PutMapping("/delivered-up-to/{id}")
    public ApiResponse<Integer> deliveredUpTo(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(messageService.markDeliveredUpTo(userId, coupleId, id));
    }

    @PutMapping("/{id}/recall")
    public ApiResponse<Void> recall(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        messageService.recall(userId, coupleId, id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        messageService.delete(userId, coupleId, id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Integer> batchDelete(@RequestBody List<Long> ids) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.ok(0);
        }
        int rows = messageService.deleteBatch(userId, coupleId, ids);
        return ApiResponse.ok(rows);
    }

    @DeleteMapping
    public ApiResponse<Integer> deleteAll() {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        int rows = messageService.deleteAll(userId, coupleId);
        return ApiResponse.ok(rows);
    }

    @GetMapping("/cursor")
    public ApiResponse<Long> getCursor(@RequestParam("deviceId") String deviceId) {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(messageService.getCursor(userId, deviceId));
    }

    @PutMapping("/cursor/{id}")
    public ApiResponse<Void> setCursor(@PathVariable("id") Long id, @RequestParam("deviceId") String deviceId) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        messageService.upsertCursor(userId, coupleId, deviceId, id);
        return ApiResponse.ok(null);
    }
}
