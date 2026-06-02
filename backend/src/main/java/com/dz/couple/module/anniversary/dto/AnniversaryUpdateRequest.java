package com.dz.couple.module.anniversary.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AnniversaryUpdateRequest {
    @NotBlank(message = "请输入名称")
    @Size(max = 64, message = "名称长度需≤64")
    private String title;

    private String date;

    private String calendarType;
    private Integer lunarMonth;
    private Integer lunarDay;
    private Boolean lunarLeap;

    @Size(max = 200, message = "备注长度需≤200")
    private String note;

    private Boolean reminderEnabled;
    private Integer reminderDaysBefore;
    private Boolean reminderOnDay;

    @Size(max = 20, message = "类型长度需≤20")
    private String type;

    @Size(max = 16, message = "图标长度需≤16")
    private String icon;

    @Size(max = 16, message = "主题色长度需≤16")
    private String themeColor;

    @Size(max = 512, message = "封面URL过长")
    private String coverUrl;

    @Size(max = 512, message = "封面缩略图URL过长")
    private String coverThumbUrl;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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
}
