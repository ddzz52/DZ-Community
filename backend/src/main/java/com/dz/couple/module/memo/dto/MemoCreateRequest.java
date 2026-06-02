package com.dz.couple.module.memo.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class MemoCreateRequest {
    @NotNull(message = "请选择分类")
    private Long categoryId;

    @NotBlank(message = "请输入标题")
    @Size(max = 30, message = "标题≤30字")
    private String title;

    @NotBlank(message = "请输入内容")
    @Size(max = 500, message = "内容≤500字")
    private String content;

    @NotNull(message = "请选择状态")
    private Integer status;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
