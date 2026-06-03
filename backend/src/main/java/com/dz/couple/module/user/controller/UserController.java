package com.dz.couple.module.user.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.common.BusinessException;
import com.dz.couple.module.user.dto.UserVO;
import com.dz.couple.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        Long userId = CurrentUser.getUserId();
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return ApiResponse.ok(userService.getById(userId));
    }

    /** 用户自助注销账号 */
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMe() {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        userService.deleteMyAccount(userId, coupleId);
        return ApiResponse.ok(null);
    }
}

