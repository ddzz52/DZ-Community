package com.dz.couple.module.diary.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.diary.dto.DiaryCommentCreateRequest;
import com.dz.couple.module.diary.dto.DiaryCommentVO;
import com.dz.couple.module.diary.dto.DiaryCreateRequest;
import com.dz.couple.module.diary.dto.DiaryUpdateRequest;
import com.dz.couple.module.diary.dto.DiaryVO;
import com.dz.couple.module.diary.service.DiaryService;
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
@RequestMapping("/api/diaries")
@Validated
public class DiaryController {
    private final DiaryService diaryService;

    @Autowired
    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @GetMapping
    public ApiResponse<List<DiaryVO>> list(@RequestParam(value = "keyword", required = false) String keyword,
                                          @RequestParam(value = "limit", required = false) Integer limit) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        int l = limit == null ? 50 : limit;
        return ApiResponse.ok(diaryService.list(userId, coupleId, keyword, l));
    }

    @PostMapping
    public ApiResponse<DiaryVO> create(@Valid @RequestBody DiaryCreateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(diaryService.create(userId, coupleId, req));
    }

    @PutMapping("/{id}")
    public ApiResponse<DiaryVO> update(@PathVariable("id") Long id, @Valid @RequestBody DiaryUpdateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(diaryService.update(userId, coupleId, id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        diaryService.delete(userId, coupleId, id);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/like")
    public ApiResponse<DiaryVO> toggleLike(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(diaryService.toggleLike(userId, coupleId, id));
    }

    @PutMapping("/{id}/favorite")
    public ApiResponse<DiaryVO> toggleFavorite(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(diaryService.toggleFavorite(userId, coupleId, id));
    }

    @PutMapping("/{id}/pin")
    public ApiResponse<DiaryVO> togglePin(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(diaryService.togglePin(userId, coupleId, id));
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<List<DiaryCommentVO>> listComments(@PathVariable("id") Long id,
                                                         @RequestParam(value = "limit", required = false) Integer limit) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        int l = limit == null ? 50 : limit;
        return ApiResponse.ok(diaryService.listComments(userId, coupleId, id, l));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<DiaryCommentVO> addComment(@PathVariable("id") Long id, @Valid @RequestBody DiaryCommentCreateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(diaryService.addComment(userId, coupleId, id, req));
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable("commentId") Long commentId) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        diaryService.deleteComment(userId, coupleId, commentId);
        return ApiResponse.ok(null);
    }
}
