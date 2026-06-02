package com.dz.couple.module.photo.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class PhotoCommentCreateRequest {
    @NotBlank(message = "请输入评论")
    @Size(max = 100, message = "评论长度需≤100")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

