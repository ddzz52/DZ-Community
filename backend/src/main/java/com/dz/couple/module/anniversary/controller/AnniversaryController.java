package com.dz.couple.module.anniversary.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.anniversary.dto.AnniversaryCreateRequest;
import com.dz.couple.module.anniversary.dto.AnniversaryUpdateRequest;
import com.dz.couple.module.anniversary.dto.AnniversaryVO;
import com.dz.couple.module.anniversary.service.AnniversaryService;
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
@RequestMapping("/api/anniversaries")
@Validated
public class AnniversaryController {
    private final AnniversaryService anniversaryService;

    @Autowired
    public AnniversaryController(AnniversaryService anniversaryService) {
        this.anniversaryService = anniversaryService;
    }

    @GetMapping
    public ApiResponse<List<AnniversaryVO>> list() {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(anniversaryService.list(coupleId));
    }

    @GetMapping("/upcoming")
    public ApiResponse<List<AnniversaryVO>> upcoming(@RequestParam(value = "days", required = false) Integer days) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        int d = days == null ? 3 : days;
        return ApiResponse.ok(anniversaryService.upcoming(coupleId, d));
    }

    @PostMapping
    public ApiResponse<AnniversaryVO> create(@Valid @RequestBody AnniversaryCreateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(anniversaryService.create(userId, coupleId, req));
    }

    @PutMapping("/{id}")
    public ApiResponse<AnniversaryVO> update(@PathVariable("id") Long id, @Valid @RequestBody AnniversaryUpdateRequest req) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(anniversaryService.update(coupleId, id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        anniversaryService.delete(coupleId, id);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/pin")
    public ApiResponse<AnniversaryVO> togglePin(@PathVariable("id") Long id) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(anniversaryService.togglePinned(coupleId, id));
    }
}

