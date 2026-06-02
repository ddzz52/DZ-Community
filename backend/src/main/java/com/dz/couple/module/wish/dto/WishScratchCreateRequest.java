package com.dz.couple.module.wish.dto;

import javax.validation.constraints.NotBlank;

public class WishScratchCreateRequest {
    @NotBlank
    private String content;

    private Integer revealMode;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getRevealMode() {
        return revealMode;
    }

    public void setRevealMode(Integer revealMode) {
        this.revealMode = revealMode;
    }
}
