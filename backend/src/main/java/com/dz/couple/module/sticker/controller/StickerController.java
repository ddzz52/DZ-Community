package com.dz.couple.module.sticker.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.sticker.dto.StickerCreateRequest;
import com.dz.couple.module.sticker.dto.StickerVO;
import com.dz.couple.module.sticker.service.StickerService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/stickers")
@Validated
public class StickerController {
    private final StickerService stickerService;

    public StickerController(StickerService stickerService) {
        this.stickerService = stickerService;
    }

    @GetMapping
    public ApiResponse<List<StickerVO>> list(@RequestParam(value = "limit", required = false) Integer limit) {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(stickerService.listMy(userId, limit == null ? 100 : limit));
    }

    @PostMapping
    public ApiResponse<StickerVO> create(@Valid @RequestBody StickerCreateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(stickerService.create(userId, coupleId, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        stickerService.delete(userId, id);
        return ApiResponse.ok(null);
    }
}

