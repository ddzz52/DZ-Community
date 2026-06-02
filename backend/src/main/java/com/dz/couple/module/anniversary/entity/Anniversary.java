package com.dz.couple.module.anniversary.entity;

import java.util.Date;

public class Anniversary {
    private Long id;
    private Long coupleId;
    private String title;
    private Date anniversaryDate;
    private String calendarType;
    private Integer lunarMonth;
    private Integer lunarDay;
    private Integer lunarLeap;
    private String type;
    private String icon;
    private String themeColor;
    private String coverUrl;
    private String coverThumbUrl;
    private String note;
    private Integer reminderEnabled;
    private Integer reminderDaysBefore;
    private Integer reminderOnDay;
    private Integer pinned;
    private Long createdBy;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getAnniversaryDate() {
        return anniversaryDate;
    }

    public void setAnniversaryDate(Date anniversaryDate) {
        this.anniversaryDate = anniversaryDate;
    }

    public String getCalendarType() {
        return calendarType;
    }

    public void setCalendarType(String calendarType) {
        this.calendarType = calendarType;
    }

    public Integer getLunarMonth() {
        return lunarMonth;
    }

    public void setLunarMonth(Integer lunarMonth) {
        this.lunarMonth = lunarMonth;
    }

    public Integer getLunarDay() {
        return lunarDay;
    }

    public void setLunarDay(Integer lunarDay) {
        this.lunarDay = lunarDay;
    }

    public Integer getLunarLeap() {
        return lunarLeap;
    }

    public void setLunarLeap(Integer lunarLeap) {
        this.lunarLeap = lunarLeap;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getThemeColor() {
        return themeColor;
    }

    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getCoverThumbUrl() {
        return coverThumbUrl;
    }

    public void setCoverThumbUrl(String coverThumbUrl) {
        this.coverThumbUrl = coverThumbUrl;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(Integer reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public Integer getReminderDaysBefore() {
        return reminderDaysBefore;
    }

    public void setReminderDaysBefore(Integer reminderDaysBefore) {
        this.reminderDaysBefore = reminderDaysBefore;
    }

    public Integer getReminderOnDay() {
        return reminderOnDay;
    }

    public void setReminderOnDay(Integer reminderOnDay) {
        this.reminderOnDay = reminderOnDay;
    }

    public Integer getPinned() {
        return pinned;
    }

    public void setPinned(Integer pinned) {
        this.pinned = pinned;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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
