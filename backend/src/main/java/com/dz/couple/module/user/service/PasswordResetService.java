package com.dz.couple.module.user.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.notification.NotificationTypes;
import com.dz.couple.module.notification.service.NotificationService;
import com.dz.couple.module.user.dto.PasswordResetConfirmRequest;
import com.dz.couple.module.user.entity.PasswordResetRequest;
import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.PasswordResetRequestMapper;
import com.dz.couple.module.user.mapper.UserMapper;
import com.dz.couple.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class PasswordResetService {
    private final UserMapper userMapper;
    private final PasswordResetRequestMapper passwordResetRequestMapper;
    private final NotificationService notificationService;
    private final PasswordUtil passwordUtil;

    @Autowired
    public PasswordResetService(UserMapper userMapper, PasswordResetRequestMapper passwordResetRequestMapper, NotificationService notificationService, PasswordUtil passwordUtil) {
        this.userMapper = userMapper;
        this.passwordResetRequestMapper = passwordResetRequestMapper;
        this.notificationService = notificationService;
        this.passwordUtil = passwordUtil;
    }

    @Transactional
    public void request(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        Long coupleId = user.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "情侣空间未初始化");
        }
        List<User> users = userMapper.listByCoupleId(coupleId);
        Long partnerId = null;
        if (users != null) {
            for (User u : users) {
                if (u != null && u.getId() != null && !u.getId().equals(user.getId())) {
                    partnerId = u.getId();
                    break;
                }
            }
        }
        if (partnerId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "尚未绑定第二个账号，无法找回密码");
        }

        String code = String.format("%06d", (int) (Math.random() * 1000000));
        PasswordResetRequest req = new PasswordResetRequest();
        req.setUserId(user.getId());
        req.setCoupleId(coupleId);
        req.setCodeHash(passwordUtil.hash(code));
        req.setExpiresAt(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)));
        passwordResetRequestMapper.insert(req);

        Date today = Date.from(java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        notificationService.create(
                partnerId,
                coupleId,
                NotificationTypes.PASSWORD_RESET,
                "密码重置验证码",
                "对方正在找回密码，验证码：" + code + "（10分钟内有效）",
                req.getId(),
                today,
                false
        );
    }

    @Transactional
    public void confirm(PasswordResetConfirmRequest req) {
        User user = userMapper.findByUsername(req.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        PasswordResetRequest latest = passwordResetRequestMapper.findLatestValid(user.getId(), new Date());
        if (latest == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码已过期或不存在");
        }
        if (!passwordUtil.matches(req.getCode(), latest.getCodeHash())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码不正确");
        }
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "两次输入的新密码不一致");
        }
        validatePassword(req.getNewPassword());
        userMapper.updatePasswordHash(user.getId(), passwordUtil.hash(req.getNewPassword()));
        passwordResetRequestMapper.markUsed(latest.getId());

        Date today = Date.from(java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        notificationService.create(
                user.getId(),
                user.getCoupleId(),
                NotificationTypes.SECURITY_PASSWORD_CHANGED,
                "密码已重置",
                "你的密码已通过验证码找回流程重置成功。",
                latest.getId(),
                today,
                true
        );
    }

    private void validatePassword(String pwd) {
        if (pwd == null || pwd.length() < 8) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码长度需≥8位且包含字母、数字、特殊符号");
        }
        boolean hasLetter = pwd.matches(".*[A-Za-z].*");
        boolean hasDigit = pwd.matches(".*\\d.*");
        boolean hasSymbol = pwd.matches(".*[^A-Za-z0-9].*");
        if (!hasLetter || !hasDigit || !hasSymbol) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码需包含字母、数字、特殊符号");
        }
    }
}
