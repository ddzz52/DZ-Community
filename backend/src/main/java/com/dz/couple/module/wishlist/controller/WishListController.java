package com.dz.couple.module.wishlist.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.wishlist.dto.WishItemCreateRequest;
import com.dz.couple.module.wishlist.dto.WishItemStatusRequest;
import com.dz.couple.module.wishlist.dto.WishItemUpdateRequest;
import com.dz.couple.module.wishlist.dto.WishItemVO;
import com.dz.couple.module.wishlist.service.WishListService;
import com.dz.couple.module.wish.dto.WishScratchVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/wish-list")
@Validated
public class WishListController {
    private final WishListService wishListService;

    @Autowired
    public WishListController(WishListService wishListService) {
        this.wishListService = wishListService;
    }

    @GetMapping
    public ApiResponse<List<WishItemVO>> list(@RequestParam(value = "status", required = false) Integer status) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Integer s = status;
        if (s != null && s != 0 && s != 1 && s != 2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status 仅支持 0/1/2");
        }
        return ApiResponse.ok(wishListService.list(coupleId, s));
    }

    @PostMapping
    public ApiResponse<WishItemVO> create(@Valid @RequestBody WishItemCreateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(wishListService.create(userId, coupleId, req));
    }

    @PutMapping("/{id}")
    public ApiResponse<WishItemVO> update(@PathVariable("id") Long id, @Valid @RequestBody WishItemUpdateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return ApiResponse.ok(wishListService.update(userId, coupleId, id, req));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<WishItemVO> changeStatus(@PathVariable("id") Long id, @RequestBody WishItemStatusRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        Integer s = req == null ? null : req.getStatus();
        if (s == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status 不能为空");
        }
        if (s == 1) return ApiResponse.ok(wishListService.markCompleted(userId, coupleId, id));
        if (s == 2) return ApiResponse.ok(wishListService.cancel(userId, coupleId, id));
        if (s == 0) return ApiResponse.ok(wishListService.reopen(userId, coupleId, id));
        throw new BusinessException(ErrorCode.BAD_REQUEST, "status 仅支持 0/1/2");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        wishListService.delete(coupleId, id);
        return ApiResponse.ok(true);
    }

    @PostMapping("/{id}/scratch")
    public ApiResponse<WishScratchVO> createScratch(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return ApiResponse.ok(wishListService.createScratchFromWish(userId, coupleId, id));
    }

    @PostMapping("/roulette")
    public ApiResponse<WishItemVO> roulette() {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(wishListService.roulette(userId, coupleId));
    }
}
