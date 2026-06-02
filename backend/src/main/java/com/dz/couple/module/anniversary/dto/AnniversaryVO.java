package com.dz.couple.module.anniversary.dto;

import java.util.Date;

public class AnniversaryVO {
    private Long id;
    private String title;
    private Date date;
    private Date nextDate;
    private Integer daysLeft;
    private String calendarType;
    private Integer lunarMonth;
    private Integer lunarDay;
    private Boolean lunarLeap;
    private String type;
    private String icon;
    private String themeColor;
    private String coverUrl;
    private String coverThumbUrl;
    private String note;
    private Boolean reminderEnabled;
    private Integer reminderDaysBefore;
    private Boolean reminderOnDay;
    private Boolean pinned;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getNextDate() {
        return nextDate;
    }

    public void setNextDate(Date nextDate) {
        this.nextDate = nextDate;
    }

    public Integer getDaysLeft() {
        return daysLeft;
    }

    public void setDaysLeft(Integer daysLeft) {
        this.daysLeft = daysLeft;
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

    public Boolean getLunarLeap() {
        return lunarLeap;
    }

    public void setLunarLeap(Boolean lunarLeap) {
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

    public Boolean getReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(Boolean reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public Integer getReminderDaysBefore() {
        return reminderDaysBefore;
    }

    public void setReminderDaysBefore(Integer reminderDaysBefore) {
        this.reminderDaysBefore = reminderDaysBefore;
    }

    public Boolean getReminderOnDay() {
        return reminderOnDay;
    }

    public void setReminderOnDay(Boolean reminderOnDay) {
        this.reminderOnDay = reminderOnDay;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }
}
