package com.dz.couple.module.album.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.album.dto.AlbumCreateRequest;
import com.dz.couple.module.album.dto.AlbumUpdateRequest;
import com.dz.couple.module.album.dto.AlbumVO;
import com.dz.couple.module.album.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/albums")
@Validated
public class AlbumController {
    private final AlbumService albumService;

    @Autowired
    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    @GetMapping
    public ApiResponse<List<AlbumVO>> list() {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(albumService.list(coupleId));
    }

    @PostMapping
    public ApiResponse<AlbumVO> create(@Valid @RequestBody AlbumCreateRequest req) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(albumService.create(coupleId, req));
    }

    @PutMapping("/{id}")
    public ApiResponse<AlbumVO> update(@PathVariable("id") Long id, @Valid @RequestBody AlbumUpdateRequest req) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(albumService.update(coupleId, id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        albumService.delete(coupleId, id);
        return ApiResponse.ok(null);
    }
}

