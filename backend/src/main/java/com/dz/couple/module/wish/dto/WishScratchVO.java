package com.dz.couple.module.wish.dto;

import java.util.Date;

public class WishScratchVO {
    private Long id;
    private Integer status;
    private String content;

    private Integer revealMode;
    private Long scratchedBy1;
    private Date scratchedAt1;
    private Long scratchedBy2;
    private Date scratchedAt2;

    private Long createdBy;
    private String createdByNickname;
    private Date createdAt;

    private Long scratchedBy;
    private String scratchedByNickname;
    private Date scratchedAt;

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

    public Integer getRevealMode() {
        return revealMode;
    }

    public void setRevealMode(Integer revealMode) {
        this.revealMode = revealMode;
    }

    public Long getScratchedBy1() {
        return scratchedBy1;
    }

    public void setScratchedBy1(Long scratchedBy1) {
        this.scratchedBy1 = scratchedBy1;
    }

    public Date getScratchedAt1() {
        return scratchedAt1;
    }

    public void setScratchedAt1(Date scratchedAt1) {
        this.scratchedAt1 = scratchedAt1;
    }

    public Long getScratchedBy2() {
        return scratchedBy2;
    }

    public void setScratchedBy2(Long scratchedBy2) {
        this.scratchedBy2 = scratchedBy2;
    }

    public Date getScratchedAt2() {
        return scratchedAt2;
    }

    public void setScratchedAt2(Date scratchedAt2) {
        this.scratchedAt2 = scratchedAt2;
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

    public Long getScratchedBy() {
        return scratchedBy;
    }

    public void setScratchedBy(Long scratchedBy) {
        this.scratchedBy = scratchedBy;
    }

    public String getScratchedByNickname() {
        return scratchedByNickname;
    }

    public void setScratchedByNickname(String scratchedByNickname) {
        this.scratchedByNickname = scratchedByNickname;
    }

    public Date getScratchedAt() {
        return scratchedAt;
    }

    public void setScratchedAt(Date scratchedAt) {
        this.scratchedAt = scratchedAt;
    }
}
