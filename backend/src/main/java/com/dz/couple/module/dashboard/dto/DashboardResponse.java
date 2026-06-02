package com.dz.couple.module.dashboard.dto;

import java.util.Date;
import java.util.List;

public class DashboardResponse {
    private Today today;
    private AnniversaryCard anniversary;
    private PhotoPreview coverPhoto;
    private Recent recent;
    private Badges badges;

    public Today getToday() {
        return today;
    }

    public void setToday(Today today) {
        this.today = today;
    }

    public AnniversaryCard getAnniversary() {
        return anniversary;
    }

    public void setAnniversary(AnniversaryCard anniversary) {
        this.anniversary = anniversary;
    }

    public PhotoPreview getCoverPhoto() {
        return coverPhoto;
    }

    public void setCoverPhoto(PhotoPreview coverPhoto) {
        this.coverPhoto = coverPhoto;
    }

    public Recent getRecent() {
        return recent;
    }

    public void setRecent(Recent recent) {
        this.recent = recent;
    }

    public Badges getBadges() {
        return badges;
    }

    public void setBadges(Badges badges) {
        this.badges = badges;
    }

    public static class Today {
        private Date date;
        private String quote;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getQuote() {
            return quote;
        }

        public void setQuote(String quote) {
            this.quote = quote;
        }
    }

    public static class AnniversaryCard {
        private Long id;
        private String title;
        private Date date;
        private Integer daysLeft;
        private Boolean pinned;
        private String type;
        private String icon;
        private String themeColor;
        private String coverUrl;
        private String coverThumbUrl;
        private String tagline;

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

        public Integer getDaysLeft() {
            return daysLeft;
        }

        public void setDaysLeft(Integer daysLeft) {
            this.daysLeft = daysLeft;
        }

        public Boolean getPinned() {
            return pinned;
        }

        public void setPinned(Boolean pinned) {
            this.pinned = pinned;
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

        public String getTagline() {
            return tagline;
        }

        public void setTagline(String tagline) {
            this.tagline = tagline;
        }
    }

    public static class Recent {
        private List<DiaryPreview> diaries;
        private List<PhotoPreview> photos;

        public List<DiaryPreview> getDiaries() {
            return diaries;
        }

        public void setDiaries(List<DiaryPreview> diaries) {
            this.diaries = diaries;
        }

        public List<PhotoPreview> getPhotos() {
            return photos;
        }

        public void setPhotos(List<PhotoPreview> photos) {
            this.photos = photos;
        }
    }

    public static class DiaryPreview {
        private Long id;
        private String contentPreview;
        private String mood;
        private Date createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getContentPreview() {
            return contentPreview;
        }

        public void setContentPreview(String contentPreview) {
            this.contentPreview = contentPreview;
        }

        public String getMood() {
            return mood;
        }

        public void setMood(String mood) {
            this.mood = mood;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class PhotoPreview {
        private Long id;
        private String url;
        private String thumbUrl;
        private Date createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getThumbUrl() {
            return thumbUrl;
        }

        public void setThumbUrl(String thumbUrl) {
            this.thumbUrl = thumbUrl;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class Badges {
        private int unreadMessages;
        private int unreadNotifications;
        private int reminders;

        public int getUnreadMessages() {
            return unreadMessages;
        }

        public void setUnreadMessages(int unreadMessages) {
            this.unreadMessages = unreadMessages;
        }

        public int getUnreadNotifications() {
            return unreadNotifications;
        }

        public void setUnreadNotifications(int unreadNotifications) {
            this.unreadNotifications = unreadNotifications;
        }

        public int getReminders() {
            return reminders;
        }

        public void setReminders(int reminders) {
            this.reminders = reminders;
        }
    }
}
