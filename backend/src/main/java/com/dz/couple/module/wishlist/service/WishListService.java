package com.dz.couple.module.wishlist.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.notification.NotificationTypes;
import com.dz.couple.module.notification.service.NotificationService;
import com.dz.couple.module.profile.entity.UserSettings;
import com.dz.couple.module.profile.mapper.UserSettingsMapper;
import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.UserMapper;
import com.dz.couple.module.wish.dto.WishScratchVO;
import com.dz.couple.module.wish.entity.WishScratchCard;
import com.dz.couple.module.wish.mapper.WishScratchMapper;
import com.dz.couple.module.wishlist.dto.WishItemCreateRequest;
import com.dz.couple.module.wishlist.dto.WishItemUpdateRequest;
import com.dz.couple.module.wishlist.dto.WishItemVO;
import com.dz.couple.module.wishlist.entity.WishItem;
import com.dz.couple.module.wishlist.mapper.WishItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class WishListService {
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_COMPLETED = 1;
    public static final int STATUS_CANCELED = 2;

    public static final int PRIORITY_HIGH = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_LOW = 2;

    public static final String SOURCE_SCRATCH_FROM = "SCRATCH_FROM";
    public static final String SOURCE_SCRATCH_CARD = "SCRATCH_CARD";

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final WishItemMapper wishItemMapper;
    private final WishScratchMapper wishScratchMapper;
    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final UserSettingsMapper userSettingsMapper;

    @Autowired
    public WishListService(WishItemMapper wishItemMapper, WishScratchMapper wishScratchMapper, NotificationService notificationService, UserMapper userMapper, UserSettingsMapper userSettingsMapper) {
        this.wishItemMapper = wishItemMapper;
        this.wishScratchMapper = wishScratchMapper;
        this.notificationService = notificationService;
        this.userMapper = userMapper;
        this.userSettingsMapper = userSettingsMapper;
    }

    public List<WishItemVO> list(Long coupleId, Integer status) {
        return wishItemMapper.list(coupleId, status);
    }

    public WishItemVO create(Long userId, Long coupleId, WishItemCreateRequest req) {
        String content = String.valueOf(req.getContent()).trim();
        if (content.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入心愿内容");
        }
        if (content.length() > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "心愿内容长度需≤100字");
        }
        String expectedAt = String.valueOf(req.getExpectedAt()).trim();
        LocalDate expected = parseDate(expectedAt);
        if (expected.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "期望完成时间需≥当前日期");
        }
        Integer p = req.getPriority();
        if (p == null) p = PRIORITY_MEDIUM;
        if (p != PRIORITY_HIGH && p != PRIORITY_MEDIUM && p != PRIORITY_LOW) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "优先级不合法");
        }
        String remark = req.getRemark() == null ? "" : String.valueOf(req.getRemark()).trim();
        if (remark.length() > 200) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "备注长度需≤200字");
        }
        String sourceType = req.getSourceType();
        Long sourceId = req.getSourceId();
        if (sourceType != null && sourceType.length() > 32) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "sourceType 不合法");
        }

        Date now = new Date();
        WishItem it = new WishItem();
        it.setCoupleId(coupleId);
        it.setContent(content);
        it.setExpectedAt(expectedAt);
        it.setPriority(p);
        it.setRemark(remark);
        it.setStatus(STATUS_PENDING);
        it.setCreatedBy(userId);
        it.setCreatedAt(now);
        it.setUpdatedBy(userId);
        it.setUpdatedAt(now);
        it.setCompletedAt(null);
        it.setCanceledAt(null);
        it.setSourceType(sourceType);
        it.setSourceId(sourceId);
        wishItemMapper.insert(it);
        return wishItemMapper.findVoById(coupleId, it.getId());
    }

    public WishScratchVO createScratchFromWish(Long userId, Long coupleId, Long id) {
        WishItem exists = wishItemMapper.findById(coupleId, id);
        if (exists == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "心愿不存在");
        }
        if (exists.getStatus() == null || exists.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "仅待完成心愿可生成刮刮卡");
        }
        if (SOURCE_SCRATCH_CARD.equals(exists.getSourceType()) && exists.getSourceId() != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "该心愿已生成过刮刮卡");
        }
        String content = String.valueOf(exists.getContent()).trim();
        if (content.length() > 50) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "心愿内容超过50字，请先精简再生成刮刮卡");
        }
        Date now = new Date();
        WishScratchCard c = new WishScratchCard();
        c.setCoupleId(coupleId);
        c.setContent(content);
        c.setStatus(0);
        c.setRevealMode(0);
        c.setCreatedBy(userId);
        c.setCreatedAt(now);
        c.setScratchedBy(null);
        c.setScratchedAt(null);
        wishScratchMapper.insert(c);
        wishItemMapper.markScratchCard(coupleId, id, SOURCE_SCRATCH_CARD, c.getId(), userId, now);
        return wishScratchMapper.findVoById(coupleId, c.getId());
    }

    public WishItemVO roulette(Long userId, Long coupleId) {
        WishItemVO picked = wishItemMapper.pickRandomPending(coupleId);
        if (picked == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "暂无待完成心愿");
        }
        List<User> users = userMapper.listByCoupleId(coupleId);
        Date today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        String p = priorityText(picked.getPriority());
        for (User u : users) {
            if (u == null || u.getId() == null) {
                continue;
            }
            UserSettings us = userSettingsMapper.findByUserId(u.getId());
            if (us != null && us.getMessageEnabled() != null && us.getMessageEnabled() == 0) {
                continue;
            }
            notificationService.create(
                    u.getId(),
                    coupleId,
                    NotificationTypes.WISH_ROULETTE,
                    "心愿轮盘抽中：" + picked.getContent(),
                    "优先级 " + p + " · 期望 " + picked.getExpectedAt(),
                    picked.getId(),
                    today,
                    true
            );
        }
        return picked;
    }

    public WishItemVO update(Long userId, Long coupleId, Long id, WishItemUpdateRequest req) {
        String content = String.valueOf(req.getContent()).trim();
        if (content.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入心愿内容");
        }
        if (content.length() > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "心愿内容长度需≤100字");
        }
        String expectedAt = String.valueOf(req.getExpectedAt()).trim();
        LocalDate expected = parseDate(expectedAt);
        if (expected.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "期望完成时间需≥当前日期");
        }
        Integer p = req.getPriority();
        if (p == null) p = PRIORITY_MEDIUM;
        if (p != PRIORITY_HIGH && p != PRIORITY_MEDIUM && p != PRIORITY_LOW) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "优先级不合法");
        }
        String remark = req.getRemark() == null ? "" : String.valueOf(req.getRemark()).trim();
        if (remark.length() > 200) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "备注长度需≤200字");
        }
        int affected = wishItemMapper.updateWhenPending(coupleId, id, content, expectedAt, p, remark, userId, new Date());
        if (affected <= 0) {
            WishItem exists = wishItemMapper.findById(coupleId, id);
            if (exists == null) throw new BusinessException(ErrorCode.NOT_FOUND, "心愿不存在");
            throw new BusinessException(ErrorCode.CONFLICT, "已完成的心愿不可修改");
        }
        return wishItemMapper.findVoById(coupleId, id);
    }

    public WishItemVO markCompleted(Long userId, Long coupleId, Long id) {
        Date now = new Date();
        int affected = wishItemMapper.markCompleted(coupleId, id, userId, now);
        if (affected <= 0) {
            WishItem exists = wishItemMapper.findById(coupleId, id);
            if (exists == null) throw new BusinessException(ErrorCode.NOT_FOUND, "心愿不存在");
            if (exists.getStatus() != null && exists.getStatus() == STATUS_COMPLETED) {
                throw new BusinessException(ErrorCode.CONFLICT, "该心愿已完成");
            }
            throw new BusinessException(ErrorCode.CONFLICT, "仅待完成心愿可标记完成");
        }
        return wishItemMapper.findVoById(coupleId, id);
    }

    public WishItemVO cancel(Long userId, Long coupleId, Long id) {
        Date now = new Date();
        int affected = wishItemMapper.cancel(coupleId, id, userId, now);
        if (affected <= 0) {
            WishItem exists = wishItemMapper.findById(coupleId, id);
            if (exists == null) throw new BusinessException(ErrorCode.NOT_FOUND, "心愿不存在");
            throw new BusinessException(ErrorCode.CONFLICT, "仅待完成心愿可取消");
        }
        return wishItemMapper.findVoById(coupleId, id);
    }

    public WishItemVO reopen(Long userId, Long coupleId, Long id) {
        Date now = new Date();
        int affected = wishItemMapper.reopenFromCanceled(coupleId, id, userId, now);
        if (affected <= 0) {
            WishItem exists = wishItemMapper.findById(coupleId, id);
            if (exists == null) throw new BusinessException(ErrorCode.NOT_FOUND, "心愿不存在");
            throw new BusinessException(ErrorCode.CONFLICT, "仅已取消心愿可恢复为待完成");
        }
        return wishItemMapper.findVoById(coupleId, id);
    }

    public void delete(Long coupleId, Long id) {
        int affected = wishItemMapper.deleteWhenPending(coupleId, id);
        if (affected <= 0) {
            WishItem exists = wishItemMapper.findById(coupleId, id);
            if (exists == null) throw new BusinessException(ErrorCode.NOT_FOUND, "心愿不存在");
            throw new BusinessException(ErrorCode.CONFLICT, "已完成/已取消的心愿不可删除");
        }
    }

    private LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s, DF);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "日期格式需为 yyyy-MM-dd");
        }
    }

    private String priorityText(Integer p) {
        if (p == null) return "中";
        if (p == PRIORITY_HIGH) return "高";
        if (p == PRIORITY_LOW) return "低";
        return "中";
    }
}
