package com.dz.couple.module.profile.dto;

import javax.validation.constraints.Size;

public class UpdateSettingsRequest {
    private Boolean messageEnabled;
    private Boolean reminderEnabled;
    @Size(max = 5, message = "时间格式应为HH:mm")
    private String reminderWindowStart;
    @Size(max = 5, message = "时间格式应为HH:mm")
    private String reminderWindowEnd;
    private Boolean dndEnabled;

    @Size(max = 5, message = "时间格式应为HH:mm")
    private String dndStart;

    @Size(max = 5, message = "时间格式应为HH:mm")
    private String dndEnd;

    public Boolean getMessageEnabled() {
        return messageEnabled;
    }

    public void setMessageEnabled(Boolean messageEnabled) {
        this.messageEnabled = messageEnabled;
    }

    public Boolean getReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(Boolean reminderEnabled) {
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

    public Boolean getDndEnabled() {
        return dndEnabled;
    }

    public void setDndEnabled(Boolean dndEnabled) {
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
}
