package com.dz.couple.module.wish.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.wish.dto.WishScratchCreateRequest;
import com.dz.couple.module.wish.dto.WishScratchVO;
import com.dz.couple.module.wish.entity.WishScratchCard;
import com.dz.couple.module.wish.mapper.WishScratchMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class WishScratchService {
    private final WishScratchMapper wishScratchMapper;

    @Autowired
    public WishScratchService(WishScratchMapper wishScratchMapper) {
        this.wishScratchMapper = wishScratchMapper;
    }

    public List<WishScratchVO> list(Long coupleId, Integer status) {
        return wishScratchMapper.list(coupleId, status);
    }

    public WishScratchVO create(Long userId, Long coupleId, WishScratchCreateRequest req) {
        String content = String.valueOf(req.getContent()).trim();
        if (content.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入心愿内容");
        }
        if (content.length() > 50) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "心愿内容长度需≤50字");
        }
        Integer mode = req.getRevealMode();
        if (mode == null) mode = 0;
        if (mode != 0 && mode != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "创建模式不合法");
        }
        Date now = new Date();
        WishScratchCard c = new WishScratchCard();
        c.setCoupleId(coupleId);
        c.setContent(content);
        c.setStatus(0);
        c.setRevealMode(mode);
        c.setCreatedBy(userId);
        c.setCreatedAt(now);
        c.setScratchedBy(null);
        c.setScratchedAt(null);
        wishScratchMapper.insert(c);
        return wishScratchMapper.findVoById(coupleId, c.getId());
    }

    @Transactional
    public WishScratchVO scratch(Long userId, Long coupleId, Long id) {
        WishScratchCard exists = wishScratchMapper.findById(coupleId, id);
        if (exists == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "刮刮卡不存在");
        }
        Integer mode = exists.getRevealMode() == null ? 0 : exists.getRevealMode();
        Date now = new Date();
        if (exists.getStatus() != null && exists.getStatus() == 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "这张刮刮卡已经被刮开啦");
        }
        if (mode == 0) {
            int affected = wishScratchMapper.markScratched(coupleId, id, userId, now);
            if (affected <= 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "刮开失败，请稍后再试");
            }
            return wishScratchMapper.findVoById(coupleId, id);
        }
        if ((exists.getScratchedBy1() != null && exists.getScratchedBy1().equals(userId)) ||
                (exists.getScratchedBy2() != null && exists.getScratchedBy2().equals(userId))) {
            throw new BusinessException(ErrorCode.CONFLICT, "你已经刮过这张刮刮卡啦");
        }
        wishScratchMapper.markScratchedBoth(coupleId, id, userId, now);
        wishScratchMapper.unlockIfBothScratched(coupleId, id, userId, now);
        return wishScratchMapper.findVoById(coupleId, id);
    }
}
