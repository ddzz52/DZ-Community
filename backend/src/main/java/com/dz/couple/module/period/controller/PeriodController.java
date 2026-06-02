package com.dz.couple.module.period.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.period.dto.ConfirmPeriodRequest;
import com.dz.couple.module.period.dto.PeriodStatusVO;
import com.dz.couple.module.period.dto.UpdatePeriodReminderRequest;
import com.dz.couple.module.period.dto.UpdatePeriodSettingsRequest;
import com.dz.couple.module.period.service.PeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/period")
public class PeriodController {
    private final PeriodService periodService;

    @Autowired
    public PeriodController(PeriodService periodService) {
        this.periodService = periodService;
    }

    @GetMapping("/status")
    public ApiResponse<PeriodStatusVO> status() {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(periodService.getStatus(coupleId));
    }

    @PutMapping("/settings")
    public ApiResponse<PeriodStatusVO> updateSettings(@RequestBody UpdatePeriodSettingsRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(periodService.updateSettings(userId, coupleId, req));
    }

    @PostMapping("/confirm")
    public ApiResponse<PeriodStatusVO> confirm(@RequestBody ConfirmPeriodRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(periodService.confirmPeriod(userId, coupleId, req));
    }

    @PatchMapping("/reminder")
    public ApiResponse<PeriodStatusVO> updateReminder(@RequestBody UpdatePeriodReminderRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(periodService.updateReminder(userId, coupleId, req));
    }
}

