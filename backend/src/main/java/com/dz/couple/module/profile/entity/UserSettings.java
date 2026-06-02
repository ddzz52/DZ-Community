package com.dz.couple.module.profile.entity;

import java.util.Date;

public class UserSettings {
    private Long userId;
    private Long coupleId;
    private Integer messageEnabled;
    private Integer reminderEnabled;
    private String reminderWindowStart;
    private String reminderWindowEnd;
    private Integer dndEnabled;
    private String dndStart;
    private String dndEnd;
    private Date createdAt;
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

    public Integer getMessageEnabled() {
        return messageEnabled;
    }

    public void setMessageEnabled(Integer messageEnabled) {
        this.messageEnabled = messageEnabled;
    }

    public Integer getReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(Integer reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public String getReminderWindowStart() {
        return reminderWindowStart;
    }

    public void setReminderWindowStart(String reminderWindowStart) {
        this.reminderWindowStart = reminderWindowStart;
    }

    public String getReminderWindowEnd() {
        return reminderWindowEnd;
    }

    public void setReminderWindowEnd(String reminderWindowEnd) {
        this.reminderWindowEnd = reminderWindowEnd;
    }

    public Integer getDndEnabled() {
        return dndEnabled;
    }

    public void setDndEnabled(Integer dndEnabled) {
        this.dndEnabled = dndEnabled;
    }

    public String getDndStart() {
        return dndStart;
    }

    public void setDndStart(String dndStart) {
        this.dndStart = dndStart;
    }

    public String getDndEnd() {
        return dndEnd;
    }

    public void setDndEnd(String dndEnd) {
        this.dndEnd = dndEnd;
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
