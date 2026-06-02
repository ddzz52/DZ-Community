package com.dz.couple.module.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 32, message = "用户名过长")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在6-64位")
    private String password;

    @Size(max = 32, message = "昵称过长")
    private String nickname;

    @NotNull(message = "相恋日期不能为空")
    private Date loveDate;

    private Integer gender;

    @Size(max = 255, message = "头像地址过长")
    private String avatarUrl;

    @Size(max = 32, message = "邀请码过长")
    private String inviteCode;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Date getLoveDate() {
        return loveDate;
    }

    public void setLoveDate(Date loveDate) {
        this.loveDate = loveDate;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }
}
