package com.dz.couple.module.admin.entity;

import java.util.Date;

/** 系统公告 — 管理员发布，登录页展示 */
public class SystemAnnouncement {
    private Long id;
    private String content;
    private Integer active;       // 1=启用 0=停用
    private Long createdBy;
    private Date createdAt;
    private Date updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getActive() { return active; }
    public void setActive(Integer active) { this.active = active; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
