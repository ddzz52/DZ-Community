package com.dz.couple.security;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Autowired
    public JwtInterceptor(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String token = auth;
        if (auth.startsWith("Bearer ")) {
            token = auth.substring("Bearer ".length()).trim();
        }
        if (token.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            Long userId = jwtUtil.parseUserId(token);
            CurrentUser.setUserId(userId);
            User user = userMapper.findById(userId);
            if (user == null || user.getCoupleId() == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            CurrentUser.setCoupleId(user.getCoupleId());
            CurrentUser.setRole(user.getRole());
            return true;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUser.clear();
    }
}
