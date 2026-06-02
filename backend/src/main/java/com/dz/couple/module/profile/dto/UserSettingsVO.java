package com.dz.couple.module.profile.dto;

public class UserSettingsVO {
    private boolean messageEnabled;
    private boolean reminderEnabled;
    private String reminderWindowStart;
    private String reminderWindowEnd;
    private boolean dndEnabled;
    private String dndStart;
    private String dndEnd;

    public boolean isMessageEnabled() {
        return messageEnabled;
    }

    public void setMessageEnabled(boolean messageEnabled) {
        this.messageEnabled = messageEnabled;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(boolean reminderEnabled) {
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

    public boolean isDndEnabled() {
        return dndEnabled;
    }

    public void setDndEnabled(boolean dndEnabled) {
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
