package com.dz.couple.module.profile.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

public class UpdateProfileRequest {
    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称过长")
    private String nickname;

    @Size(max = 255, message = "头像地址过长")
    private String avatarUrl;

    private Integer gender;

    @NotNull(message = "相恋日期不能为空")
    private Date loveDate;

    @Size(max = 32, message = "星座名称过长")
    private String zodiac;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public Date getLoveDate() {
        return loveDate;
    }

    public void setLoveDate(Date loveDate) {
        this.loveDate = loveDate;
    }

    public String getZodiac() {
        return zodiac;
    }

    public void setZodiac(String zodiac) {
        this.zodiac = zodiac;
    }
}

