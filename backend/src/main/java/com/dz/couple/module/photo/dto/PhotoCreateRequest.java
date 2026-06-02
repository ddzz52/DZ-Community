package com.dz.couple.module.photo.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class PhotoCreateRequest {
    @NotBlank(message = "请填写图片地址")
    @Size(max = 512, message = "图片地址过长")
    private String url;

    @Size(max = 512, message = "缩略图地址过长")
    private String thumbUrl;

    private Long albumId;

    private String shotAt;

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

    public Long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Long albumId) {
        this.albumId = albumId;
    }

    public String getShotAt() {
        return shotAt;
    }

    public void setShotAt(String shotAt) {
        this.shotAt = shotAt;
    }
}

