package com.dz.couple.module.user.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CacheService;
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
    private final LoginRateLimitService rateLimitService;
    private final CacheService cacheService;

    @Autowired
    public UserService(UserMapper userMapper, PasswordUtil passwordUtil, JwtUtil jwtUtil, CoupleMapper coupleMapper, NotificationService notificationService, LoginRateLimitService rateLimitService, CacheService cacheService) {
        this.userMapper = userMapper;
        this.passwordUtil = passwordUtil;
        this.jwtUtil = jwtUtil;
        this.coupleMapper = coupleMapper;
        this.notificationService = notificationService;
        this.rateLimitService = rateLimitService;
        this.cacheService = cacheService;
    }

    /**
     * 注册逻辑：
     * - 提供有效邀请码 → 加入该情侣空间（需空间未满）
     * - 未提供邀请码 → 创建全新情侣空间
     * - 情侣空间第一个人自动成为 ADMIN
     */
    @Transactional
    public UserVO register(String username, String password, String nickname, Date loveDate, Integer gender, String avatarUrl, String inviteCode) {
        // 检查用户名唯一性
        User exist = userMapper.findByUsername(username);
        if (exist != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }

        Couple couple;
        boolean isFirstMember = false;

        if (inviteCode != null && !inviteCode.trim().isEmpty()) {
            // 通过邀请码加入已有空间
            couple = coupleMapper.findByInviteCode(inviteCode.trim());
            if (couple == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "邀请码无效，请检查后重试");
            }
            long memberCount = userMapper.countByCoupleId(couple.getId());
            if (memberCount >= 2) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "该情侣空间已满（最多2人）");
            }
        } else {
            // 未提供邀请码 → 创建新空间
            couple = new Couple();
            couple.setInviteCode(generateInviteCode());
            coupleMapper.insert(couple);
            isFirstMember = true;
        }

        // 判断角色：仅系统第一个注册用户为全局 ADMIN，其余均为 USER
        long totalUsers = userMapper.countAll();
        String role = (totalUsers == 0) ? "ADMIN" : "USER";

        User user = new User();
        user.setCoupleId(couple.getId());
        user.setUsername(username);
        user.setPasswordHash(passwordUtil.hash(password));
        user.setNickname((nickname == null || nickname.trim().isEmpty()) ? username : nickname.trim());
        user.setRole(role);
        user.setLoveDate(loveDate);
        user.setGender(gender);
        user.setAvatarUrl(avatarUrl);
        userMapper.insert(user);

        UserVO vo = toVO(user);
        // 仅当用户是空间创建者（第一个人）时返回邀请码
        if (isFirstMember || "ADMIN".equals(role)) {
            vo.setInviteCode(couple.getInviteCode());
        }
        return vo;
    }

    /**
     * 登录后绑定邀请码：将当前用户从旧空间迁移到目标空间
     * 用于用户在注册时未填邀请码，登录后补填的场景
     */
    @Transactional
    public UserVO bindInviteCode(Long userId, Long currentCoupleId, String inviteCode) {
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邀请码不能为空");
        }

        User me = userMapper.findById(userId);
        if (me == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        // 查找目标空间
        Couple targetCouple = coupleMapper.findByInviteCode(inviteCode.trim());
        if (targetCouple == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "邀请码无效");
        }

        // 不能绑定自己的空间
        if (targetCouple.getId().equals(currentCoupleId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能绑定自己的邀请码");
        }

        // 目标空间人数检查
        long targetMemberCount = userMapper.countByCoupleId(targetCouple.getId());
        if (targetMemberCount >= 2) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "目标情侣空间已满（最多2人）");
        }

        // 检查当前空间是否只有自己（如果是，迁移后需要清理旧空间）
        long currentMemberCount = userMapper.countByCoupleId(currentCoupleId);

        // 执行绑定：更新用户的 coupleId
        userMapper.updateCoupleId(userId, targetCouple.getId());

        // 如果旧空间只剩自己，迁移后旧空间变空，清理旧空间
        if (currentMemberCount <= 1) {
            coupleMapper.deleteById(currentCoupleId);
        }

        // 清除旧空间和新空间的缓存（成员变更导致 profile/dashboard 数据失效）
        evictCaches(currentCoupleId);
        evictCaches(targetCouple.getId());

        // 刷新用户数据并返回
        User updated = userMapper.findById(userId);
        UserVO vo = toVO(updated);
        return vo;
    }

    public LoginResponse login(String username, String password, String ip, String ua) {
        // 登录频率检查
        rateLimitService.check(ip);

        User user = userMapper.findByUsername(username);
        if (user == null || !passwordUtil.matches(password, user.getPasswordHash())) {
            // 记录失败
            rateLimitService.recordFailure(ip);
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
        // 登录成功，清除失败计数
        rateLimitService.clearOnSuccess(ip);

        LoginResponse resp = new LoginResponse();
        resp.setToken(jwtUtil.createToken(user.getId()));
        resp.setUser(toVO(user));
        return resp;
    }

    /** 用户自助注销账号 — 软删除（清理个人数据，保留空间给伴侣） */
    @Transactional
    public void deleteMyAccount(Long userId, Long coupleId) {
        User me = userMapper.findById(userId);
        if (me == null || me.getCoupleId() == null || !me.getCoupleId().equals(coupleId))
            throw new BusinessException(ErrorCode.UNAUTHORIZED);

        long memberCount = userMapper.countByCoupleId(coupleId);
        if (memberCount <= 1) {
            // 最后一人：删除空间 + 用户
            coupleMapper.deleteById(coupleId);
        }
        userMapper.deleteById(userId);
    }

    private String safeShort(String s, int max) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        if (t.isEmpty()) return null;
        if (t.length() <= max) return t;
        return t.substring(0, max);
    }

    public UserVO getById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return toVO(user);
    }

    /** 清除指定 couple 的 Profile 和 Dashboard 缓存 */
    private void evictCaches(Long coupleId) {
        if (coupleId == null) return;
        cacheService.deletePattern("cache:profile:" + coupleId + ":*");
        cacheService.deletePattern("cache:dashboard:" + coupleId + ":*");
    }

    /**
     * 生成8位邀请码（排除易混淆字符 0/O/1/I/l）
     */
    private String generateInviteCode() {
        String raw = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        // 移除易混淆字符后取前8位
        String cleaned = raw.replaceAll("[0O1IL]", "");
        if (cleaned.length() < 8) {
            // fallback：重新生成
            return generateInviteCode();
        }
        return cleaned.substring(0, 8);
    }

    public UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setCoupleId(user.getCoupleId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setGender(user.getGender());
        vo.setRole(user.getRole());
        vo.setLoveDate(user.getLoveDate());
        vo.setZodiac(user.getZodiac());
        vo.setSignature(user.getSignature());
        vo.setTempSignature(user.getTempSignature());
        vo.setSignatureExpireTime(user.getSignatureExpireTime());
        return vo;
    }
}
