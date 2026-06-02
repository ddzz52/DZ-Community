package com.dz.couple.module.message.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class MessageCreateRequest {
    private String type;
    @NotBlank(message = "请输入内容")
    @Size(max = 500, message = "内容长度需≤500")
    private String content;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

