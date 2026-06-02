package com.dz.couple.module.diary.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class DiaryCommentCreateRequest {
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

