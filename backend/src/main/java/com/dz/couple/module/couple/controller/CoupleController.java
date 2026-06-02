package com.dz.couple.module.couple.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.couple.entity.Couple;
import com.dz.couple.module.couple.mapper.CoupleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/couple")
public class CoupleController {

    @Autowired
    private CoupleMapper coupleMapper;

    /** 获取"关于我们" */
    @GetMapping("/about")
    public ApiResponse<Map<String, String>> getAbout() {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Couple couple = coupleMapper.findById(coupleId);
        String text = couple != null && couple.getAboutText() != null ? couple.getAboutText() : "";
        return ApiResponse.ok(Collections.singletonMap("text", text));
    }

    /** 获取本情侣空间的邀请码 */
    @GetMapping("/invite-code")
    public ApiResponse<Map<String, String>> getInviteCode() {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Couple couple = coupleMapper.findById(coupleId);
        return ApiResponse.ok(Collections.singletonMap("inviteCode",
                couple != null && couple.getInviteCode() != null ? couple.getInviteCode() : ""));
    }

    /** 获取月度预算 */
    @GetMapping("/budget")
    public ApiResponse<Map<String, Object>> getBudget() {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        Couple couple = coupleMapper.findById(coupleId);
        return ApiResponse.ok(Collections.singletonMap("monthlyBudget",
                couple != null && couple.getMonthlyBudget() != null ? couple.getMonthlyBudget() : null));
    }

    /** 设置月度预算 */
    @PutMapping("/budget")
    public ApiResponse<Void> setBudget(@RequestBody Map<String, Object> body) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        Object val = body.get("monthlyBudget");
        java.math.BigDecimal budget = null;
        if (val instanceof Number) budget = java.math.BigDecimal.valueOf(((Number) val).doubleValue());
        coupleMapper.updateMonthlyBudget(coupleId, budget);
        return ApiResponse.ok(null);
    }

    /** 保存"关于我们" */
    @PutMapping("/about")
    public ApiResponse<Void> saveAbout(@RequestBody Map<String, String> body) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String text = body.getOrDefault("text", "");
        coupleMapper.updateAboutText(coupleId, text);
        return ApiResponse.ok(null);
    }
}
