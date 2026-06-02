package com.dz.couple.module.user.entity;

import java.util.Date;

public class User {
    private Long id;
    private Long coupleId;
    private String username;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private Date loveDate;
    private String zodiac;
    private String signature;
    private String tempSignature;
    private Date signatureExpireTime;
    private Date createdAt;
    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCoupleId() {
        return coupleId;
    }

    public void setCoupleId(Long coupleId) {
        this.coupleId = coupleId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

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

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getTempSignature() {
        return tempSignature;
    }

    public void setTempSignature(String tempSignature) {
        this.tempSignature = tempSignature;
    }

    public Date getSignatureExpireTime() {
        return signatureExpireTime;
    }

    public void setSignatureExpireTime(Date signatureExpireTime) {
        this.signatureExpireTime = signatureExpireTime;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
