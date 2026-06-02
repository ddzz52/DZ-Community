package com.dz.couple.module.message.entity;

import java.util.Date;

public class MessageCursor {
    private Long userId;
    private Long coupleId;
    private String deviceId;
    private Long lastReadId;
    private Date updatedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCoupleId() {
        return coupleId;
    }

    public void setCoupleId(Long coupleId) {
        this.coupleId = coupleId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getLastReadId() {
        return lastReadId;
    }

    public void setLastReadId(Long lastReadId) {
        this.lastReadId = lastReadId;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}

