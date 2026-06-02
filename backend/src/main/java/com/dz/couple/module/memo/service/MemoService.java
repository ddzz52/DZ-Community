package com.dz.couple.module.memo.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.memo.dto.MemoCategoryCreateRequest;
import com.dz.couple.module.memo.dto.MemoCategoryUpdateRequest;
import com.dz.couple.module.memo.dto.MemoCategoryVO;
import com.dz.couple.module.memo.dto.MemoCreateRequest;
import com.dz.couple.module.memo.dto.MemoStatusUpdateRequest;
import com.dz.couple.module.memo.dto.MemoUpdateRequest;
import com.dz.couple.module.memo.dto.MemoVO;
import com.dz.couple.module.memo.entity.Memo;
import com.dz.couple.module.memo.entity.MemoCategory;
import com.dz.couple.module.memo.mapper.MemoCategoryMapper;
import com.dz.couple.module.memo.mapper.MemoMapper;
import com.dz.couple.module.message.ws.ChatHub;
import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MemoService {
    private final MemoMapper memoMapper;
    private final MemoCategoryMapper memoCategoryMapper;
    private final UserMapper userMapper;
    private final ChatHub chatHub;
    private final MemoEditLockManager lockManager;

    @Autowired
    public MemoService(MemoMapper memoMapper,
                       MemoCategoryMapper memoCategoryMapper,
                       UserMapper userMapper,
                       ChatHub chatHub,
                       MemoEditLockManager lockManager) {
        this.memoMapper = memoMapper;
        this.memoCategoryMapper = memoCategoryMapper;
        this.userMapper = userMapper;
        this.chatHub = chatHub;
        this.lockManager = lockManager;
    }

    public List<MemoCategoryVO> listCategories(Long userId, Long coupleId) {
        ensureMember(userId, coupleId);
        ensureDefaultCategories(userId, coupleId);
        List<MemoCategoryVO> list = memoCategoryMapper.listByCoupleId(coupleId);
        return list == null ? Collections.<MemoCategoryVO>emptyList() : list;
    }

    public MemoCategoryVO createCategory(Long userId, Long coupleId, MemoCategoryCreateRequest req) {
        ensureMember(userId, coupleId);
        ensureDefaultCategories(userId, coupleId);
        String name = safeTrim(req == null ? null : req.getName());
        if (name == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入分类名称");
        }
        MemoCategory exists = memoCategoryMapper.findByName(coupleId, name);
        if (exists != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "分类已存在");
        }
        Date now = new Date();
        MemoCategory c = new MemoCategory();
        c.setCoupleId(coupleId);
        c.setName(name);
        c.setSortNo(1000);
        c.setSystemFlag(0);
        c.setCreatedBy(userId);
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        memoCategoryMapper.insert(c);
        pushCategoryChanged(coupleId, userId, "CREATED", c.getId());
        return toCategoryVO(c);
    }

    public MemoCategoryVO updateCategory(Long userId, Long coupleId, Long id, MemoCategoryUpdateRequest req) {
        ensureMember(userId, coupleId);
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        String name = safeTrim(req == null ? null : req.getName());
        if (name == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入分类名称");
        }
        MemoCategory cur = memoCategoryMapper.findById(coupleId, id);
        if (cur == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (cur.getSystemFlag() != null && cur.getSystemFlag() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统分类不可修改");
        }
        MemoCategory exists = memoCategoryMapper.findByName(coupleId, name);
        if (exists != null && exists.getId() != null && !exists.getId().equals(id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "分类已存在");
        }
        int n = memoCategoryMapper.updateName(coupleId, id, name, new Date());
        if (n <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        pushCategoryChanged(coupleId, userId, "UPDATED", id);
        MemoCategoryVO out = new MemoCategoryVO();
        out.setId(id);
        out.setName(name);
        out.setSortNo(cur.getSortNo());
        out.setSystemFlag(cur.getSystemFlag());
        out.setCreatedBy(cur.getCreatedBy());
        out.setCreatedAt(cur.getCreatedAt());
        out.setUpdatedAt(new Date());
        return out;
    }

    public void deleteCategory(Long userId, Long coupleId, Long id) {
        ensureMember(userId, coupleId);
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        MemoCategory cur = memoCategoryMapper.findById(coupleId, id);
        if (cur == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (cur.getSystemFlag() != null && cur.getSystemFlag() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统分类不可删除");
        }
        long used = memoMapper.countByCategoryId(coupleId, id);
        if (used > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该分类下还有备忘录，无法删除");
        }
        int n = memoCategoryMapper.delete(coupleId, id);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        pushCategoryChanged(coupleId, userId, "DELETED", id);
    }

    public List<MemoVO> listMemos(Long userId,
                                 Long coupleId,
                                 Long categoryId,
                                 Integer status,
                                 String q,
                                 Integer beforeStatus,
                                 Date beforeAt,
                                 Long beforeId,
                                 int limit) {
        ensureMember(userId, coupleId);
        int l = Math.max(1, Math.min(limit, 200));
        List<MemoVO> list = memoMapper.list(coupleId, categoryId, status, q, beforeStatus, beforeAt, beforeId, l);
        return list == null ? Collections.<MemoVO>emptyList() : list;
    }

    public MemoVO createMemo(Long userId, Long coupleId, MemoCreateRequest req) {
        ensureMember(userId, coupleId);
        if (req == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        Long categoryId = req.getCategoryId();
        MemoCategory cat = memoCategoryMapper.findById(coupleId, categoryId);
        if (cat == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择分类");
        }
        Integer status = normalizeStatus(req.getStatus());
        String title = safeTrim(req.getTitle());
        String content = safeTrim(req.getContent());
        if (title == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入标题");
        }
        if (content == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入内容");
        }
        Date now = new Date();
        Memo m = new Memo();
        m.setCoupleId(coupleId);
        m.setCategoryId(categoryId);
        m.setTitle(title);
        m.setContent(content);
        m.setStatus(status);
        m.setCreatedBy(userId);
        m.setUpdatedBy(userId);
        m.setCreatedAt(now);
        m.setUpdatedAt(now);
        memoMapper.insert(m);
        pushMemoChanged(coupleId, userId, "CREATED", m.getId());
        MemoVO vo = memoMapper.findVoById(coupleId, m.getId());
        return vo == null ? null : vo;
    }

    public MemoVO updateMemo(Long userId, Long coupleId, Long id, MemoUpdateRequest req) {
        ensureMember(userId, coupleId);
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        ensureNotLockedByOther(coupleId, id, userId);
        Memo cur = memoMapper.findById(coupleId, id);
        if (cur == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (req == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        Long categoryId = req.getCategoryId();
        MemoCategory cat = memoCategoryMapper.findById(coupleId, categoryId);
        if (cat == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择分类");
        }
        Integer status = normalizeStatus(req.getStatus());
        String title = safeTrim(req.getTitle());
        String content = safeTrim(req.getContent());
        if (title == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入标题");
        }
        if (content == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入内容");
        }
        Date now = new Date();
        cur.setCategoryId(categoryId);
        cur.setTitle(title);
        cur.setContent(content);
        cur.setStatus(status);
        cur.setUpdatedBy(userId);
        cur.setUpdatedAt(now);
        int n = memoMapper.update(cur);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        pushMemoChanged(coupleId, userId, "UPDATED", id);
        MemoVO vo = memoMapper.findVoById(coupleId, id);
        return vo == null ? null : vo;
    }

    public MemoVO updateStatus(Long userId, Long coupleId, Long id, MemoStatusUpdateRequest req) {
        ensureMember(userId, coupleId);
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        ensureNotLockedByOther(coupleId, id, userId);
        Integer status = normalizeStatus(req == null ? null : req.getStatus());
        Date now = new Date();
        int n = memoMapper.updateStatus(coupleId, id, status, userId, now);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        pushMemoChanged(coupleId, userId, "STATUS", id);
        MemoVO vo = memoMapper.findVoById(coupleId, id);
        return vo == null ? null : vo;
    }

    public void deleteMemo(Long userId, Long coupleId, Long id) {
        ensureMember(userId, coupleId);
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        ensureNotLockedByOther(coupleId, id, userId);
        int n = memoMapper.delete(coupleId, id);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        lockManager.release(coupleId, id, userId);
        pushMemoChanged(coupleId, userId, "DELETED", id);
        pushEditingState(coupleId, id, false, userId);
    }

    public MemoEditLockManager.TryLockResult tryStartEditing(Long userId, Long coupleId, Long memoId) {
        ensureMember(userId, coupleId);
        if (memoId == null) {
            return new MemoEditLockManager.TryLockResult(false, null);
        }
        Memo cur = memoMapper.findById(coupleId, memoId);
        if (cur == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        User u = userMapper.findById(userId);
        String nick = u == null ? null : u.getNickname();
        MemoEditLockManager.TryLockResult r = lockManager.tryLock(coupleId, memoId, userId, nick, 15000);
        if (r != null && r.isOk()) {
            pushEditingState(coupleId, memoId, true, userId);
        }
        return r;
    }

    public void heartbeatEditing(Long userId, Long coupleId, Long memoId) {
        ensureMember(userId, coupleId);
        if (memoId == null) return;
        lockManager.heartbeat(coupleId, memoId, userId, 15000);
    }

    public void endEditing(Long userId, Long coupleId, Long memoId) {
        ensureMember(userId, coupleId);
        if (memoId == null) return;
        lockManager.release(coupleId, memoId, userId);
        pushEditingState(coupleId, memoId, false, userId);
    }

    private void ensureDefaultCategories(Long userId, Long coupleId) {
        long cnt = memoCategoryMapper.countByCoupleId(coupleId);
        if (cnt > 0) return;
        Date now = new Date();
        insertSystemCategory(coupleId, "日常", 10, userId, now);
        insertSystemCategory(coupleId, "约会", 20, userId, now);
        insertSystemCategory(coupleId, "购物", 30, userId, now);
        insertSystemCategory(coupleId, "其他", 40, userId, now);
    }

    private void insertSystemCategory(Long coupleId, String name, int sortNo, Long userId, Date now) {
        MemoCategory exists = memoCategoryMapper.findByName(coupleId, name);
        if (exists != null) return;
        MemoCategory c = new MemoCategory();
        c.setCoupleId(coupleId);
        c.setName(name);
        c.setSortNo(sortNo);
        c.setSystemFlag(1);
        c.setCreatedBy(userId == null ? 0L : userId);
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        memoCategoryMapper.insert(c);
    }

    private void ensureNotLockedByOther(Long coupleId, Long memoId, Long userId) {
        MemoEditLockManager.LockInfo lock = lockManager.getValidLock(coupleId, memoId);
        if (lock == null) return;
        if (lock.getUserId() != null && !lock.getUserId().equals(userId)) {
            String nick = lock.getNickname();
            if (nick != null && !nick.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.CONFLICT, "对方正在编辑（" + nick.trim() + "），请稍后操作");
            }
            throw new BusinessException(ErrorCode.CONFLICT, "对方正在编辑，请稍后操作");
        }
    }

    private void pushMemoChanged(Long coupleId, Long actorUserId, String action, Long memoId) {
        if (coupleId == null || actorUserId == null || memoId == null) {
            return;
        }
        List<User> users = userMapper.listByCoupleId(coupleId);
        if (users == null || users.isEmpty()) {
            return;
        }
        User actor = userMapper.findById(actorUserId);
        Map<String, Object> data = new HashMap<>();
        data.put("action", action);
        data.put("id", memoId);
        data.put("byUserId", actorUserId);
        data.put("byNickname", actor == null ? null : actor.getNickname());
        data.put("at", System.currentTimeMillis());

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "MEMO_CHANGED");
        payload.put("data", data);

        for (User u : users) {
            if (u == null || u.getId() == null) continue;
            chatHub.push(u.getId(), payload);
        }
    }

    private void pushCategoryChanged(Long coupleId, Long actorUserId, String action, Long categoryId) {
        if (coupleId == null || actorUserId == null || categoryId == null) {
            return;
        }
        List<User> users = userMapper.listByCoupleId(coupleId);
        if (users == null || users.isEmpty()) {
            return;
        }
        User actor = userMapper.findById(actorUserId);
        Map<String, Object> data = new HashMap<>();
        data.put("action", action);
        data.put("id", categoryId);
        data.put("byUserId", actorUserId);
        data.put("byNickname", actor == null ? null : actor.getNickname());
        data.put("at", System.currentTimeMillis());

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "MEMO_CATEGORY_CHANGED");
        payload.put("data", data);

        for (User u : users) {
            if (u == null || u.getId() == null) continue;
            chatHub.push(u.getId(), payload);
        }
    }

    private void pushEditingState(Long coupleId, Long memoId, boolean editing, Long actorUserId) {
        if (coupleId == null || memoId == null || actorUserId == null) {
            return;
        }
        List<User> users = userMapper.listByCoupleId(coupleId);
        if (users == null || users.isEmpty()) {
            return;
        }
        User actor = userMapper.findById(actorUserId);
        Map<String, Object> data = new HashMap<>();
        data.put("id", memoId);
        data.put("editing", editing);
        data.put("byUserId", actorUserId);
        data.put("byNickname", actor == null ? null : actor.getNickname());
        data.put("at", System.currentTimeMillis());

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "MEMO_EDITING");
        payload.put("data", data);

        for (User u : users) {
            if (u == null || u.getId() == null) continue;
            chatHub.push(u.getId(), payload);
        }
    }

    private MemoCategoryVO toCategoryVO(MemoCategory c) {
        if (c == null) return null;
        MemoCategoryVO vo = new MemoCategoryVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setSortNo(c.getSortNo());
        vo.setSystemFlag(c.getSystemFlag());
        vo.setCreatedBy(c.getCreatedBy());
        vo.setCreatedAt(c.getCreatedAt());
        vo.setUpdatedAt(c.getUpdatedAt());
        return vo;
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择状态");
        }
        if (status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态错误");
        }
        return status;
    }

    private void ensureMember(Long userId, Long coupleId) {
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        User u = userMapper.findById(userId);
        if (u == null || u.getCoupleId() == null || !u.getCoupleId().equals(coupleId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
