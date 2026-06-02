package com.dz.couple.module.photo.dto;

import javax.validation.constraints.NotEmpty;
import java.util.List;

public class PhotoBatchMoveRequest {
    @NotEmpty(message = "请选择照片")
    private List<Long> ids;
    private Long albumId;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    public Long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Long albumId) {
        this.albumId = albumId;
    }
}

