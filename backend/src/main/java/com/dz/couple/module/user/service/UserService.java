package com.dz.couple.module.user.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.couple.entity.Couple;
import com.dz.couple.module.couple.mapper.CoupleMapper;
import com.dz.couple.module.notification.NotificationTypes;
import com.dz.couple.module.notification.service.NotificationService;
import com.dz.couple.module.user.dto.LoginResponse;
import com.dz.couple.module.user.dto.UserVO;
import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.UserMapper;
import com.dz.couple.security.JwtUtil;
import com.dz.couple.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;
    private final CoupleMapper coupleMapper;
    private final NotificationService notificationService;

    @Autowired
    public UserService(UserMapper userMapper, PasswordUtil passwordUtil, JwtUtil jwtUtil, CoupleMapper coupleMapper, NotificationService notificationService) {
        this.userMapper = userMapper;
        this.passwordUtil = passwordUtil;
        this.jwtUtil = jwtUtil;
        this.coupleMapper = coupleMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public UserVO register(String username, String password, String nickname, Date loveDate, Integer gender, String avatarUrl, String inviteCode) {
        User exist = userMapper.findByUsername(username);
        if (exist != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }

        Couple couple = null;
        if (inviteCode != null && !inviteCode.trim().isEmpty()) {
            couple = coupleMapper.findByInviteCode(inviteCode.trim());
            if (couple == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "邀请码无效");
            }
            long memberCount = userMapper.countByCoupleId(couple.getId());
            if (memberCount >= 2) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "该情侣空间已满（最多2人）");
            }
        } else {
            couple = coupleMapper.findAvailable();
            if (couple == null) {
                couple = new Couple();
                couple.setInviteCode(UUID.randomUUID().toString().replace("-", "").substring(0, 8));
                coupleMapper.insert(couple);
            }
        }

        User user = new User();
        user.setCoupleId(couple.getId());
        user.setUsername(username);
        user.setPasswordHash(passwordUtil.hash(password));
        user.setNickname((nickname == null || nickname.trim().isEmpty()) ? username : nickname.trim());
        user.setLoveDate(loveDate);
        user.setGender(gender);
        user.setAvatarUrl(avatarUrl);
        userMapper.insert(user);

        UserVO vo = toVO(user);
        vo.setInviteCode(couple.getInviteCode());
        return vo;
    }

    public LoginResponse login(String username, String password, String ip, String ua) {
        User user = userMapper.findByUsername(username);
        if (user == null || !passwordUtil.matches(password, user.getPasswordHash())) {
            if (user != null && user.getId() != null && user.getCoupleId() != null) {
                Date today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
                String ipText = safeShort(ip, 64);
                String uaText = safeShort(ua, 120);
                String content = "检测到一次登录失败尝试。";
                if (ipText != null) {
                    content += " IP：" + ipText;
                }
                if (uaText != null) {
                    content += " 设备：" + uaText;
                }
                notificationService.create(
                        user.getId(),
                        user.getCoupleId(),
                        NotificationTypes.SECURITY_LOGIN_ANOMALY,
                        "登录异常提醒",
                        content,
                        user.getId(),
                        today,
                        true
                );
            }
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        LoginResponse resp = new LoginResponse();
        resp.setToken(jwtUtil.createToken(user.getId()));
        resp.setUser(toVO(user));
        return resp;
    }

    private String safeShort(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim().replaceAll("\\s+", " ");
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max);
    }

    public UserVO getById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return toVO(user);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setCoupleId(user.getCoupleId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setGender(user.getGender());
        vo.setLoveDate(user.getLoveDate());
        return vo;
    }
}
