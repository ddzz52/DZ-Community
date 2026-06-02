package com.dz.couple.module.memo.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.memo.dto.MemoCategoryCreateRequest;
import com.dz.couple.module.memo.dto.MemoCategoryUpdateRequest;
import com.dz.couple.module.memo.dto.MemoCategoryVO;
import com.dz.couple.module.memo.dto.MemoCreateRequest;
import com.dz.couple.module.memo.dto.MemoStatusUpdateRequest;
import com.dz.couple.module.memo.dto.MemoUpdateRequest;
import com.dz.couple.module.memo.dto.MemoVO;
import com.dz.couple.module.memo.service.MemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/memos")
@Validated
public class MemoController {
    private final MemoService memoService;

    @Autowired
    public MemoController(MemoService memoService) {
        this.memoService = memoService;
    }

    @GetMapping
    public ApiResponse<List<MemoVO>> list(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                         @RequestParam(value = "status", required = false) Integer status,
                                         @RequestParam(value = "q", required = false) String q,
                                         @RequestParam(value = "beforeStatus", required = false) Integer beforeStatus,
                                         @RequestParam(value = "beforeAt", required = false) String beforeAt,
                                         @RequestParam(value = "beforeId", required = false) Long beforeId,
                                         @RequestParam(value = "limit", required = false) Integer limit) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        int l = limit == null ? 50 : limit;
        return ApiResponse.ok(memoService.listMemos(userId, coupleId, categoryId, status, q, beforeStatus, parseDateTime(beforeAt), beforeId, l));
    }

    @PostMapping
    public ApiResponse<MemoVO> create(@Valid @RequestBody MemoCreateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(memoService.createMemo(userId, coupleId, req));
    }

    @PutMapping("/{id}")
    public ApiResponse<MemoVO> update(@PathVariable("id") Long id, @Valid @RequestBody MemoUpdateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(memoService.updateMemo(userId, coupleId, id, req));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<MemoVO> updateStatus(@PathVariable("id") Long id, @Valid @RequestBody MemoStatusUpdateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(memoService.updateStatus(userId, coupleId, id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        memoService.deleteMemo(userId, coupleId, id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/categories")
    public ApiResponse<List<MemoCategoryVO>> categories() {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(memoService.listCategories(userId, coupleId));
    }

    @PostMapping("/categories")
    public ApiResponse<MemoCategoryVO> createCategory(@Valid @RequestBody MemoCategoryCreateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(memoService.createCategory(userId, coupleId, req));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<MemoCategoryVO> updateCategory(@PathVariable("id") Long id, @Valid @RequestBody MemoCategoryUpdateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(memoService.updateCategory(userId, coupleId, id, req));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        memoService.deleteCategory(userId, coupleId, id);
        return ApiResponse.ok(null);
    }

    private Date parseDateTime(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        String t = s.trim().replace('T', ' ');
        if (t.endsWith("Z")) t = t.substring(0, t.length() - 1);
        int dot = t.indexOf('.');
        if (dot > 0) t = t.substring(0, dot);
        try {
            if (t.length() == 10) {
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
                return f.parse(t);
            }
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return f.parse(t);
        } catch (ParseException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "时间格式错误");
        }
    }
}
