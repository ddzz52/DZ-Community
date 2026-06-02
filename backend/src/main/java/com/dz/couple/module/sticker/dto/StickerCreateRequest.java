package com.dz.couple.module.sticker.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class StickerCreateRequest {
    @NotBlank(message = "请上传图片")
    @Size(max = 512, message = "URL过长")
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

