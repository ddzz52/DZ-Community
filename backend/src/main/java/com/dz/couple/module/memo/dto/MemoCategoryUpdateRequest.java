package com.dz.couple.module.memo.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class MemoCategoryUpdateRequest {
    @NotBlank(message = "请输入分类名称")
    @Size(max = 20, message = "分类名称≤20字")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
