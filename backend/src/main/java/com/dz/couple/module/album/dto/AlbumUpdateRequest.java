package com.dz.couple.module.album.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AlbumUpdateRequest {
    @NotBlank(message = "请输入分类名")
    @Size(max = 20, message = "分类名长度需≤20")
    private String name;

    private Integer sortNo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }
}

