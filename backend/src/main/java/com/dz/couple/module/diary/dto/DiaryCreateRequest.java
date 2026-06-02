package com.dz.couple.module.diary.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

public class DiaryCreateRequest {
    @NotBlank(message = "请输入内容")
    @Size(max = 2000, message = "文字长度需≤2000")
    private String content;

    @Size(max = 16, message = "心情长度需≤16")
    private String mood;

    private Boolean privateFlag;

    private List<String> images;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public Boolean getPrivateFlag() {
        return privateFlag;
    }

    public void setPrivateFlag(Boolean privateFlag) {
        this.privateFlag = privateFlag;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}

