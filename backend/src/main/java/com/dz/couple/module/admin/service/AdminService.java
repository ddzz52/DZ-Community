package com.dz.couple.module.admin.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.couple.mapper.CoupleMapper;
import com.dz.couple.module.user.dto.UserVO;
import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {
    private final UserMapper userMapper;
    private final CoupleMapper coupleMapper;

    @Autowired
    public AdminService(UserMapper userMapper, CoupleMapper coupleMapper) {
        this.userMapper = userMapper;
        this.coupleMapper = coupleMapper;
    }

    /** 管理员查看所有用户 */
    public List<UserVO> listAllUsers(Long adminUserId) {
        ensureAdmin(adminUserId);
        List<User> users = userMapper.listAll();
        List<UserVO> vos = new ArrayList<>();
        for (User u : users) {
            vos.add(toVO(u));
        }
        return vos;
    }

    /** 管理员删除用户 */
    @Transactional
    public void deleteUser(Long adminUserId, Long targetUserId) {
        ensureAdmin(adminUserId);
        if (adminUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能删除自己");
        }
        User target = userMapper.findById(targetUserId);
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        // 检查目标用户所在空间人数
        long memberCount = userMapper.countByCoupleId(target.getCoupleId());
        userMapper.deleteById(targetUserId);
        // 如果删除后空间为空，清理空间
        if (memberCount <= 1) {
            coupleMapper.deleteById(target.getCoupleId());
        }
    }

    /** 管理员修改用户角色 */
    @Transactional
    public UserVO updateUserRole(Long adminUserId, Long targetUserId, String role) {
        ensureAdmin(adminUserId);
        if (!"ADMIN".equals(role) && !"USER".equals(role)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色只能是 ADMIN 或 USER");
        }
        User target = userMapper.findById(targetUserId);
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        userMapper.updateRole(targetUserId, role);
        User updated = userMapper.findById(targetUserId);
        return toVO(updated);
    }

    private void ensureAdmin(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可执行此操作");
        }
    }

    private UserVO toVO(User user) {
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
        return vo;
    }
}
