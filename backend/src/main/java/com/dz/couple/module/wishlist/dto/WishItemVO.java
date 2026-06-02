package com.dz.couple.module.wishlist.dto;

import java.util.Date;

public class WishItemVO {
    private Long id;
    private Integer status;
    private String content;
    private String expectedAt;
    private Integer priority;
    private String remark;
    private Date completedAt;
    private Date canceledAt;
    private Long createdBy;
    private String createdByNickname;
    private Date createdAt;
    private Long updatedBy;
    private String updatedByNickname;
    private Date updatedAt;
    private String sourceType;
    private Long sourceId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

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

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public Date getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(Date canceledAt) {
        this.canceledAt = canceledAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByNickname() {
        return createdByNickname;
    }

    public void setCreatedByNickname(String createdByNickname) {
        this.createdByNickname = createdByNickname;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedByNickname() {
        return updatedByNickname;
    }

    public void setUpdatedByNickname(String updatedByNickname) {
        this.updatedByNickname = updatedByNickname;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }
}
