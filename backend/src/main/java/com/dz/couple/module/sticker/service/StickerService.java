package com.dz.couple.module.sticker.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.sticker.dto.StickerCreateRequest;
import com.dz.couple.module.sticker.dto.StickerVO;
import com.dz.couple.module.sticker.entity.Sticker;
import com.dz.couple.module.sticker.mapper.StickerMapper;
import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class StickerService {
    private final StickerMapper stickerMapper;
    private final UserMapper userMapper;

    public StickerService(StickerMapper stickerMapper, UserMapper userMapper) {
        this.stickerMapper = stickerMapper;
        this.userMapper = userMapper;
    }

    public List<StickerVO> listMy(Long userId, int limit) {
        ensureUser(userId);
        int l = Math.max(1, Math.min(limit, 200));
        List<Sticker> list = stickerMapper.listByUserId(userId, l);
        List<StickerVO> out = new ArrayList<>();
        if (list != null) {
            for (Sticker s : list) {
                StickerVO vo = new StickerVO();
                vo.setId(s.getId());
                vo.setUrl(s.getUrl());
                vo.setCreatedAt(s.getCreatedAt());
                out.add(vo);
            }
        }
        return out;
    }

    @Transactional
    public StickerVO create(Long userId, Long coupleId, StickerCreateRequest req) {
        ensureMember(userId, coupleId);
        String url = safeTrim(req == null ? null : req.getUrl());
        if (url == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传图片");
        }
        if (!(url.startsWith("/uploads/") || url.startsWith("http://") || url.startsWith("https://"))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片地址非法");
        }
        Sticker s = new Sticker();
        s.setUserId(userId);
        s.setCoupleId(coupleId);
        s.setUrl(url);
        s.setCreatedAt(new Date());
        stickerMapper.insert(s);
        StickerVO vo = new StickerVO();
        vo.setId(s.getId());
        vo.setUrl(s.getUrl());
        vo.setCreatedAt(s.getCreatedAt());
        return vo;
    }

    public void delete(Long userId, Long id) {
        ensureUser(userId);
        int n = stickerMapper.deleteById(userId, id);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    private void ensureUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        User u = userMapper.findById(userId);
        if (u == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
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
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

