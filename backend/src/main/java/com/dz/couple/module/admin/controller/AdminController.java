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

    /** 管理员：查看所有用户列表 */
    @GetMapping("/users")
    public ApiResponse<List<UserVO>> listUsers() {
        return ApiResponse.ok(adminService.listAllUsers(CurrentUser.getUserId()));
    }

    /** 管理员：删除用户 */
    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(CurrentUser.getUserId(), userId);
        return ApiResponse.ok(null);
    }

    /** 管理员：修改用户角色 */
    @PutMapping("/users/{userId}/role")
    public ApiResponse<UserVO> updateUserRole(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(adminService.updateUserRole(CurrentUser.getUserId(), userId, body.get("role")));
    }
}
