package com.dz.couple.module.photo.dto;

import javax.validation.constraints.Size;

public class PhotoUpdateRequest {
    private Long albumId;

    @Size(max = 64, message = "标题长度需≤64")
    private String title;

    @Size(max = 64, message = "地点长度需≤64")
    private String location;

    @Size(max = 16, message = "心情长度需≤16")
    private String mood;

    @Size(max = 255, message = "标签过长")
    private String tags;

    private String shotAt;

    public Long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Long albumId) {
        this.albumId = albumId;
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

    public String getShotAt() {
        return shotAt;
    }

    public void setShotAt(String shotAt) {
        this.shotAt = shotAt;
    }
}

