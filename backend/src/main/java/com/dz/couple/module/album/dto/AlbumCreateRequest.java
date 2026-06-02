package com.dz.couple.module.album.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AlbumCreateRequest {
    @NotBlank(message = "请输入分类名")
    @Size(max = 20, message = "分类名长度需≤20")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

