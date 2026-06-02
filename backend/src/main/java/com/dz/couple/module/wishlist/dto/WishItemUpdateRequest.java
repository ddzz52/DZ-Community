package com.dz.couple.module.wishlist.dto;

import javax.validation.constraints.NotBlank;

public class WishItemUpdateRequest {
    @NotBlank
    private String content;

    @NotBlank
    private String expectedAt;

    private Integer priority;

    private String remark;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExpectedAt() {
        return expectedAt;
    }

    public void setExpectedAt(String expectedAt) {
        this.expectedAt = expectedAt;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
