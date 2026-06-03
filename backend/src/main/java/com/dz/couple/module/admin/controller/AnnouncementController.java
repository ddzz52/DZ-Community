package com.dz.couple.module.admin.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.admin.entity.SystemAnnouncement;
import com.dz.couple.module.admin.mapper.SystemAnnouncementMapper;
import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class AnnouncementController {

    @Autowired
    private SystemAnnouncementMapper announcementMapper;

    @Autowired
    private UserMapper userMapper;

    /** 公开接口：获取生效中的公告（登录页展示，无需认证） */
    @GetMapping("/api/announcements/active")
    public ApiResponse<List<Map<String, Object>>> listActive() {
        List<SystemAnnouncement> list = announcementMapper.listActive();
        List<Map<String, Object>> result = new ArrayList<>();
        for (SystemAnnouncement a : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("content", a.getContent());
            m.put("createdAt", a.getCreatedAt());
            result.add(m);
        }
        return ApiResponse.ok(result);
    }

    /** 管理员：公告列表 */
    @GetMapping("/api/admin/announcements")
    public ApiResponse<List<SystemAnnouncement>> listAll() {
        ensureAdmin();
        return ApiResponse.ok(announcementMapper.listAll());
    }

    /** 管理员：发布公告 */
    @PostMapping("/api/admin/announcements")
    public ApiResponse<SystemAnnouncement> create(@RequestBody Map<String, String> body) {
        ensureAdmin();
        String content = body.get("content");
        if (content == null || content.trim().isEmpty())
            throw new BusinessException(ErrorCode.BAD_REQUEST, "公告内容不能为空");
        if (content.length() > 500)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "公告内容不能超过500字");
        SystemAnnouncement a = new SystemAnnouncement();
        a.setContent(content.trim());
        a.setActive(1);
        a.setCreatedBy(CurrentUser.getUserId());
        announcementMapper.insert(a);
        return ApiResponse.ok(a);
    }

    /** 管理员：切换公告状态 */
    @PutMapping("/api/admin/announcements/{id}/toggle")
    public ApiResponse<Void> toggle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ensureAdmin();
        int active = (body.get("active") instanceof Boolean && (Boolean) body.get("active")) ? 1 : 0;
        announcementMapper.toggleActive(id, active);
        return ApiResponse.ok(null);
    }

    /** 管理员：删除公告 */
    @DeleteMapping("/api/admin/announcements/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ensureAdmin();
        announcementMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    private void ensureAdmin() {
        Long userId = CurrentUser.getUserId();
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        User user = userMapper.findById(userId);
        if (user == null || !"ADMIN".equals(user.getRole()))
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可执行此操作");
    }
}
