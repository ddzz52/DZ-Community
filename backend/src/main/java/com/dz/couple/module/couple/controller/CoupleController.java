package com.dz.couple.module.couple.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.couple.entity.Couple;
import com.dz.couple.module.couple.mapper.CoupleMapper;
import com.dz.couple.module.user.dto.UserVO;
import com.dz.couple.module.user.mapper.UserMapper;
import com.dz.couple.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/couple")
public class CoupleController {

    @Autowired
    private CoupleMapper coupleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

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

    /**
     * 登录后绑定邀请码：将当前用户迁移到目标情侣空间
     * 用户注册时若未填邀请码，登录后可在此补填
     */
    @PostMapping("/bind")
    public ApiResponse<UserVO> bindInviteCode(@RequestBody Map<String, String> body) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String inviteCode = body.get("inviteCode");
        return ApiResponse.ok(userService.bindInviteCode(userId, coupleId, inviteCode));
    }

    /** 验证邀请码是否有效（不执行绑定） */
    @GetMapping("/check-invite")
    public ApiResponse<Map<String, Object>> checkInviteCode(@RequestParam("code") String code) {
        if (code == null || code.trim().isEmpty()) {
            return ApiResponse.ok(Collections.singletonMap("valid", false));
        }
        Couple couple = coupleMapper.findByInviteCode(code.trim());
        if (couple == null) {
            return ApiResponse.ok(Collections.singletonMap("valid", false));
        }
        long memberCount = userMapper.countByCoupleId(couple.getId());
        boolean valid = memberCount < 2;
        return ApiResponse.ok(Map.of("valid", valid, "memberCount", memberCount));
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
