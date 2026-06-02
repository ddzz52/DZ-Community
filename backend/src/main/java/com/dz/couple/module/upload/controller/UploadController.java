package com.dz.couple.module.upload.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.upload.dto.UploadResult;
import com.dz.couple.module.upload.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {
    private final UploadService uploadService;

    @Autowired
    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/images")
    public ApiResponse<List<UploadResult>> uploadImages(@RequestParam("files") MultipartFile[] files,
                                                       @RequestParam(value = "biz", required = false) String biz,
                                                       @RequestParam(value = "albumId", required = false) Long albumId) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (files == null || files.length == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择图片");
        }
        if (files.length > 10) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "一次最多上传10张");
        }
        List<UploadResult> out = new ArrayList<>();
        for (MultipartFile f : files) {
            UploadResult r = new UploadResult();
            r.setUrl(uploadService.saveImage(f, coupleId, biz, albumId));
            r.setThumbUrl(null);
            out.add(r);
        }
        return ApiResponse.ok(out);
    }

    @PostMapping("/audios")
    public ApiResponse<List<UploadResult>> uploadAudios(@RequestParam("files") MultipartFile[] files,
                                                       @RequestParam(value = "biz", required = false) String biz) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (files == null || files.length == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择音频");
        }
        if (files.length > 3) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "一次最多上传3个");
        }
        List<UploadResult> out = new ArrayList<>();
        for (MultipartFile f : files) {
            UploadResult r = new UploadResult();
            r.setUrl(uploadService.saveAudio(f, coupleId, biz));
            r.setThumbUrl(null);
            out.add(r);
        }
        return ApiResponse.ok(out);
    }
}
