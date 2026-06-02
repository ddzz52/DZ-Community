package com.dz.couple.module.notification.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.notification.dto.NotificationVO;
import com.dz.couple.module.notification.service.NotificationService;
import com.dz.couple.module.period.service.PeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final PeriodService periodService;

    @Autowired
    public NotificationController(NotificationService notificationService, PeriodService periodService) {
        this.notificationService = notificationService;
        this.periodService = periodService;
    }

    @GetMapping
    public ApiResponse<List<NotificationVO>> list(@RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly,
                                                 @RequestParam(value = "limit", required = false) Integer limit) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        notificationService.ensureAnniversaryReminders(userId, coupleId);
        periodService.ensurePeriodReminders(coupleId);
        return ApiResponse.ok(notificationService.listMy(userId, unreadOnly != null && unreadOnly, limit == null ? 50 : limit));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        notificationService.markRead(userId, id);
        return ApiResponse.ok(null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markReadAll() {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        notificationService.markReadAll(userId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/clear-read")
    public ApiResponse<Integer> clearRead() {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(notificationService.clearRead(userId));
    }
}
