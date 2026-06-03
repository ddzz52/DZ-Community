package com.dz.couple.module.admin.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.module.admin.service.AdminService;
import com.dz.couple.module.user.dto.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** 系统概览统计 */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(adminService.getSystemStats(CurrentUser.getUserId()));
    }

    /** 用户列表 */
    @GetMapping("/users")
    public ApiResponse<List<UserVO>> listUsers() {
        return ApiResponse.ok(adminService.listAllUsers(CurrentUser.getUserId()));
    }

    /** 删除用户 */
    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(CurrentUser.getUserId(), userId);
        return ApiResponse.ok(null);
    }

    /** 修改用户角色 */
    @PutMapping("/users/{userId}/role")
    public ApiResponse<UserVO> updateUserRole(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(adminService.updateUserRole(CurrentUser.getUserId(), userId, body.get("role")));
    }

    /** 修改用户登录名 */
    @PutMapping("/users/{userId}/username")
    public ApiResponse<UserVO> updateUsername(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(adminService.updateUsername(CurrentUser.getUserId(), userId, body.get("username")));
    }

    /** 强制密码重置 — 返回6位验证码 */
    @PostMapping("/users/{userId}/force-reset")
    public ApiResponse<Map<String, String>> forceResetPassword(@PathVariable Long userId) {
        return ApiResponse.ok(adminService.forceResetPassword(CurrentUser.getUserId(), userId));
    }
}
