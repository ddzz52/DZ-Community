package com.dz.couple.module.profile.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.profile.dto.ChangePasswordRequest;
import com.dz.couple.module.profile.dto.ProfileResponse;
import com.dz.couple.module.profile.dto.UpdateMySignatureRequest;
import com.dz.couple.module.profile.dto.UpdateProfileRequest;
import com.dz.couple.module.profile.dto.UpdateSettingsRequest;
import com.dz.couple.module.profile.dto.UpdateSignatureRequest;
import com.dz.couple.module.profile.dto.UpdateTempSignatureRequest;
import com.dz.couple.module.profile.dto.UserSettingsVO;
import com.dz.couple.module.profile.service.ProfileService;
import com.dz.couple.module.user.dto.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;

    @Autowired
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ApiResponse<ProfileResponse> get() {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(profileService.getProfile(userId, coupleId));
    }

    @PutMapping("/me")
    public ApiResponse<UserVO> updateMe(@Valid @RequestBody UpdateProfileRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(profileService.updateMe(userId, coupleId, req));
    }

    @PutMapping("/signature")
    public ApiResponse<Void> updateSignature(@Valid @RequestBody UpdateSignatureRequest req) {
        Long coupleId = CurrentUser.getCoupleId();
        if (coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        profileService.updateSignature(coupleId, req.getSignature());
        return ApiResponse.ok(null);
    }

    @PutMapping("/my-signature")
    public ApiResponse<UserVO> updateMySignature(@Valid @RequestBody UpdateMySignatureRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(profileService.updateMySignature(userId, coupleId, req));
    }

    @PutMapping("/temp-signature")
    public ApiResponse<UserVO> updateTempSignature(@Valid @RequestBody UpdateTempSignatureRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(profileService.updateTempSignature(userId, coupleId, req));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        profileService.changePassword(userId, coupleId, req);
        return ApiResponse.ok(null);
    }

    @GetMapping("/settings")
    public ApiResponse<UserSettingsVO> getSettings() {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(profileService.getSettings(userId, coupleId));
    }

    @PutMapping("/settings")
    public ApiResponse<UserSettingsVO> updateSettings(@Valid @RequestBody UpdateSettingsRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(profileService.updateSettings(userId, coupleId, req));
    }
}
