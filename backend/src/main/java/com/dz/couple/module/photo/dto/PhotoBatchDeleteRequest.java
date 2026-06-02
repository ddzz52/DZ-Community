package com.dz.couple.module.photo.dto;

import javax.validation.constraints.NotEmpty;
import java.util.List;

public class PhotoBatchDeleteRequest {
    @NotEmpty(message = "请选择照片")
    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}

