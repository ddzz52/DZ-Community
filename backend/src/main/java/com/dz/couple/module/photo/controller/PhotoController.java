package com.dz.couple.module.photo.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.photo.dto.PhotoBatchDeleteRequest;
import com.dz.couple.module.photo.dto.PhotoBatchMoveRequest;
import com.dz.couple.module.photo.dto.PhotoCreateRequest;
import com.dz.couple.module.photo.dto.PhotoUpdateRequest;
import com.dz.couple.module.photo.dto.PhotoCommentCreateRequest;
import com.dz.couple.module.photo.dto.PhotoCommentVO;
import com.dz.couple.module.photo.dto.PhotoVO;
import com.dz.couple.module.photo.service.PhotoService;
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
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/photos")
@Validated
public class PhotoController {
    private final PhotoService photoService;

    @Autowired
    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @GetMapping
    public ApiResponse<List<PhotoVO>> list(@RequestParam(value = "albumId", required = false) Long albumId,
                                          @RequestParam(value = "from", required = false) String from,
                                          @RequestParam(value = "to", required = false) String to,
                                          @RequestParam(value = "deletedOnly", required = false) Boolean deletedOnly,
                                          @RequestParam(value = "order", required = false) String order,
                                          @RequestParam(value = "limit", required = false) Integer limit) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        int l = limit == null ? 60 : limit;
        boolean d = deletedOnly != null && deletedOnly;
        Boolean asc = null;
        if (order != null && !order.trim().isEmpty()) {
            String o = order.trim().toLowerCase();
            if ("asc".equals(o)) asc = true;
            else if ("desc".equals(o)) asc = false;
        }
        return ApiResponse.ok(photoService.list(userId, coupleId, albumId, parseDate(from), parseDate(to), d, asc, l));
    }

    @PostMapping
    public ApiResponse<List<PhotoVO>> create(@Valid @RequestBody List<PhotoCreateRequest> reqs) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(photoService.createBatch(userId, coupleId, reqs));
    }

    @PostMapping("/batch/delete")
    public ApiResponse<Integer> batchDelete(@Valid @RequestBody PhotoBatchDeleteRequest req) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(photoService.deleteBatch(coupleId, req.getIds()));
    }

    @PostMapping("/batch/move")
    public ApiResponse<Integer> batchMove(@Valid @RequestBody PhotoBatchMoveRequest req) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(photoService.moveBatch(coupleId, req.getIds(), req.getAlbumId()));
    }

    @GetMapping("/download")
    public void download(@RequestParam("ids") String ids, HttpServletResponse resp) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        List<Long> list = parseIds(ids);
        resp.setContentType("application/zip");
        resp.setHeader("Content-Disposition", "attachment; filename=\"photos.zip\"");
        try {
            photoService.writeZip(coupleId, list, resp.getOutputStream());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "下载失败");
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<PhotoVO> update(@PathVariable("id") Long id, @Valid @RequestBody PhotoUpdateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(photoService.update(userId, coupleId, id, req));
    }

    @PutMapping("/{id}/cover")
    public ApiResponse<PhotoVO> toggleCover(@PathVariable("id") Long id) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(photoService.toggleCover(coupleId, id));
    }

    @PutMapping("/{id}/like")
    public ApiResponse<PhotoVO> toggleLike(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(photoService.toggleLike(userId, coupleId, id));
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<List<PhotoCommentVO>> listComments(@PathVariable("id") Long id,
                                                         @RequestParam(value = "limit", required = false) Integer limit) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        int l = limit == null ? 100 : limit;
        return ApiResponse.ok(photoService.listComments(coupleId, id, l));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<PhotoCommentVO> addComment(@PathVariable("id") Long id, @Valid @RequestBody PhotoCommentCreateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(photoService.addComment(userId, coupleId, id, req));
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Void> deleteComment(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        photoService.deleteComment(userId, coupleId, id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        photoService.delete(coupleId, id);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/restore")
    public ApiResponse<Void> restore(@PathVariable("id") Long id) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        photoService.restore(coupleId, id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}/purge")
    public ApiResponse<Void> purge(@PathVariable("id") Long id) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        photoService.purge(coupleId, id);
        return ApiResponse.ok(null);
    }

    private Date parseDate(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(s.trim());
        } catch (ParseException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "日期格式需为 yyyy-MM-dd");
        }
    }

    private List<Long> parseIds(String s) {
        if (s == null || s.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择照片");
        }
        String[] parts = s.split(",");
        List<Long> out = new ArrayList<>();
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (t.isEmpty()) continue;
            try {
                out.add(Long.parseLong(t));
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "照片ID格式错误");
            }
        }
        if (out.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择照片");
        }
        if (out.size() > 200) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "一次最多下载200张");
        }
        return out;
    }
}
