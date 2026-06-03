package com.dz.couple.module.user.dto;

import java.util.Date;

public class UserVO {
    private Long id;
    private Long coupleId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private String role;
    private Date loveDate;
    private String zodiac;
    private String signature;
    private String tempSignature;
    private Date signatureExpireTime;
    private String effectiveSignature;
    private String inviteCode;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCoupleId() { return coupleId; }
    public void setCoupleId(Long coupleId) { this.coupleId = coupleId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Date getLoveDate() { return loveDate; }
    public void setLoveDate(Date loveDate) { this.loveDate = loveDate; }

    public String getZodiac() { return zodiac; }
    public void setZodiac(String zodiac) { this.zodiac = zodiac; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getTempSignature() { return tempSignature; }
    public void setTempSignature(String tempSignature) { this.tempSignature = tempSignature; }

    public Date getSignatureExpireTime() { return signatureExpireTime; }
    public void setSignatureExpireTime(Date signatureExpireTime) { this.signatureExpireTime = signatureExpireTime; }

    public String getEffectiveSignature() { return effectiveSignature; }
    public void setEffectiveSignature(String effectiveSignature) { this.effectiveSignature = effectiveSignature; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
}
