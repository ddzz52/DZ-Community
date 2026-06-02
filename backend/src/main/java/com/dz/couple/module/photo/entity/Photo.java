package com.dz.couple.module.photo.entity;

import java.util.Date;

public class Photo {
    private Long id;
    private Long coupleId;
    private Long uploaderId;
    private Long albumId;
    private String url;
    private String thumbUrl;
    private String title;
    private String location;
    private String mood;
    private String tags;
    private Integer coverFlag;
    private Date coverSetAt;
    private Date shotAt;
    private Integer deletedFlag;
    private Date deletedAt;
    private Long deletedFromAlbumId;
    private Date createdAt;

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

    public Long getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(Long uploaderId) {
        this.uploaderId = uploaderId;
    }

    public Long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Long albumId) {
        this.albumId = albumId;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getCoverFlag() {
        return coverFlag;
    }

    public void setCoverFlag(Integer coverFlag) {
        this.coverFlag = coverFlag;
    }

    public Date getCoverSetAt() {
        return coverSetAt;
    }

    public void setCoverSetAt(Date coverSetAt) {
        this.coverSetAt = coverSetAt;
    }

    public Date getShotAt() {
        return shotAt;
    }

    public void setShotAt(Date shotAt) {
        this.shotAt = shotAt;
    }

    public Integer getDeletedFlag() {
        return deletedFlag;
    }

    public void setDeletedFlag(Integer deletedFlag) {
        this.deletedFlag = deletedFlag;
    }

    public Date getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Date deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getDeletedFromAlbumId() {
        return deletedFromAlbumId;
    }

    public void setDeletedFromAlbumId(Long deletedFromAlbumId) {
        this.deletedFromAlbumId = deletedFromAlbumId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
