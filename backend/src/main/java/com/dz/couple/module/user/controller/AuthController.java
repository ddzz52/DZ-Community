package com.dz.couple.module.user.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.module.user.dto.LoginRequest;
import com.dz.couple.module.user.dto.LoginResponse;
import com.dz.couple.module.user.dto.PasswordResetConfirmRequest;
import com.dz.couple.module.user.dto.PasswordResetCreateRequest;
import com.dz.couple.module.user.dto.RegisterRequest;
import com.dz.couple.module.user.dto.UserVO;
import com.dz.couple.module.user.service.PasswordResetService;
import com.dz.couple.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @Autowired
    public AuthController(UserService userService, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ApiResponse<UserVO> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok(userService.register(
                req.getUsername(),
                req.getPassword(),
                req.getNickname(),
                req.getLoveDate(),
                req.getGender(),
                req.getAvatarUrl(),
                req.getInviteCode()
        ));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpReq) {
        String ip = httpReq == null ? null : httpReq.getRemoteAddr();
        String ua = httpReq == null ? null : httpReq.getHeader("User-Agent");
        return ApiResponse.ok(userService.login(req.getUsername(), req.getPassword(), ip, ua));
    }

    @PostMapping("/password-reset/request")
    public ApiResponse<Void> passwordResetRequest(@Valid @RequestBody PasswordResetCreateRequest req) {
        passwordResetService.request(req.getUsername());
        return ApiResponse.ok(null);
    }

    @PostMapping("/password-reset/confirm")
    public ApiResponse<Void> passwordResetConfirm(@Valid @RequestBody PasswordResetConfirmRequest req) {
        passwordResetService.confirm(req);
        return ApiResponse.ok(null);
    }
}
