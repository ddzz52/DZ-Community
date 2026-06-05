package com.dz.couple.module.admin.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CacheService;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.couple.mapper.CoupleMapper;
import com.dz.couple.module.user.dto.UserVO;
import com.dz.couple.module.user.entity.PasswordResetRequest;
import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.PasswordResetRequestMapper;
import com.dz.couple.module.user.mapper.UserMapper;
import com.dz.couple.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;

@Service
public class AdminService {
    private final UserMapper userMapper;
    private final CoupleMapper coupleMapper;
    private final PasswordResetRequestMapper resetRequestMapper;
    private final PasswordUtil passwordUtil;
    private final CacheService cacheService;

    @Autowired
    public AdminService(UserMapper userMapper, CoupleMapper coupleMapper, PasswordResetRequestMapper resetRequestMapper, PasswordUtil passwordUtil, CacheService cacheService) {
        this.userMapper = userMapper;
        this.coupleMapper = coupleMapper;
        this.resetRequestMapper = resetRequestMapper;
        this.passwordUtil = passwordUtil;
        this.cacheService = cacheService;
    }

    /** 系统概览统计 */
    public Map<String, Object> getSystemStats(Long adminUserId) {
        ensureAdmin(adminUserId);
        List<User> allUsers = userMapper.listAll();
        long totalUsers = allUsers.size();
        long adminCount = allUsers.stream().filter(u -> "ADMIN".equals(u.getRole())).count();
        long coupleCount = allUsers.stream().map(User::getCoupleId).filter(Objects::nonNull).distinct().count();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalCouples", coupleCount);
        stats.put("adminCount", adminCount);
        return stats;
    }

    /** 管理员查看所有用户 */
    public List<UserVO> listAllUsers(Long adminUserId) {
        ensureAdmin(adminUserId);
        List<User> users = userMapper.listAll();
        List<UserVO> vos = new ArrayList<>();
        for (User u : users) vos.add(toVO(u));
        return vos;
    }

    /** 管理员删除用户 */
    @Transactional
    public void deleteUser(Long adminUserId, Long targetUserId) {
        ensureAdmin(adminUserId);
        if (adminUserId.equals(targetUserId)) throw new BusinessException(ErrorCode.BAD_REQUEST, "不能删除自己");
        User target = userMapper.findById(targetUserId);
        if (target == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        Long targetCoupleId = target.getCoupleId();
        long memberCount = userMapper.countByCoupleId(targetCoupleId);
        userMapper.deleteById(targetUserId);
        if (memberCount <= 1) coupleMapper.deleteById(targetCoupleId);
        evictCaches(targetCoupleId);
    }

    /** 管理员修改用户角色 */
    @Transactional
    public UserVO updateUserRole(Long adminUserId, Long targetUserId, String role) {
        ensureAdmin(adminUserId);
        if (!"ADMIN".equals(role) && !"USER".equals(role))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色只能是 ADMIN 或 USER");
        User target = userMapper.findById(targetUserId);
        if (target == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        userMapper.updateRole(targetUserId, role);
        evictCaches(target.getCoupleId());
        return toVO(userMapper.findById(targetUserId));
    }

    /** 管理员修改用户登录名 */
    @Transactional
    public UserVO updateUsername(Long adminUserId, Long targetUserId, String newUsername) {
        ensureAdmin(adminUserId);
        if (newUsername == null || newUsername.trim().isEmpty() || newUsername.trim().length() > 32)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名需1-32位");
        User target = userMapper.findById(targetUserId);
        if (target == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        User conflict = userMapper.findByUsername(newUsername.trim());
        if (conflict != null && !conflict.getId().equals(targetUserId))
            throw new BusinessException(ErrorCode.CONFLICT, "该用户名已被占用");
        userMapper.updateUsername(targetUserId, newUsername.trim());
        evictCaches(target.getCoupleId());
        return toVO(userMapper.findById(targetUserId));
    }

    /** 管理员强制密码重置 — 直接设置新密码 */
    @Transactional
    public Map<String, String> forceResetPassword(Long adminUserId, Long targetUserId) {
        ensureAdmin(adminUserId);
        User target = userMapper.findById(targetUserId);
        if (target == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        // 生成6位随机验证码
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));
        String codeHash = passwordUtil.hash(code);

        // 使旧重置请求失效
        resetRequestMapper.deactivateByUserId(targetUserId);

        // 创建新重置请求，24小时有效
        PasswordResetRequest req = new PasswordResetRequest();
        req.setUserId(targetUserId);
        req.setCoupleId(target.getCoupleId());
        req.setCodeHash(codeHash);
        req.setExpiresAt(new Date(System.currentTimeMillis() + 24 * 3600 * 1000));
        req.setUsedFlag(0);
        resetRequestMapper.insert(req);

        evictCaches(target.getCoupleId());

        Map<String, String> result = new LinkedHashMap<>();
        result.put("resetCode", code);
        result.put("username", target.getUsername());
        return result;
    }

    private void ensureAdmin(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null || !"ADMIN".equals(user.getRole()))
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可执行此操作");
    }

    /** 清除指定 couple 的 Profile 和 Dashboard 缓存，防止串号 */
    private void evictCaches(Long coupleId) {
        if (coupleId == null) return;
        cacheService.deletePattern("cache:profile:" + coupleId + ":*");
        cacheService.deletePattern("cache:dashboard:" + coupleId + ":*");
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId()); vo.setCoupleId(user.getCoupleId());
        vo.setUsername(user.getUsername()); vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl()); vo.setGender(user.getGender());
        vo.setRole(user.getRole()); vo.setLoveDate(user.getLoveDate());
        vo.setZodiac(user.getZodiac()); vo.setSignature(user.getSignature());
        return vo;
    }
}
