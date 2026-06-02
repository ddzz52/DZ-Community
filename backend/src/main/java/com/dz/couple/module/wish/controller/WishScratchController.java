package com.dz.couple.module.wish.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.wish.dto.WishScratchCreateRequest;
import com.dz.couple.module.wish.dto.WishScratchVO;
import com.dz.couple.module.wish.service.WishScratchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/wishes/scratch")
@Validated
public class WishScratchController {
    private final WishScratchService wishScratchService;

    @Autowired
    public WishScratchController(WishScratchService wishScratchService) {
        this.wishScratchService = wishScratchService;
    }

    @GetMapping
    public ApiResponse<List<WishScratchVO>> list(@RequestParam(value = "status", required = false) Integer status) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Integer s = status;
        if (s != null && s != 0 && s != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status 仅支持 0/1");
        }
        return ApiResponse.ok(wishScratchService.list(coupleId, s));
    }

    @PostMapping
    public ApiResponse<WishScratchVO> create(@Valid @RequestBody WishScratchCreateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(wishScratchService.create(userId, coupleId, req));
    }

    @PostMapping("/{id}/scratch")
    public ApiResponse<WishScratchVO> scratch(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return ApiResponse.ok(wishScratchService.scratch(userId, coupleId, id));
    }
}
