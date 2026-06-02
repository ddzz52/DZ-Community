package com.dz.couple.module.message.entity;

import java.util.Date;

public class Message {
    private Long id;
    private Long coupleId;
    private Long senderId;
    private String msgType;
    private String content;
    private Date deliveredAt;
    private Date readAt;
    private Integer recalledFlag;
    private Long recalledBy;
    private Date recalledAt;
    private Date createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCoupleId() {
        return coupleId;
    }

    public void setCoupleId(Long coupleId) {
        this.coupleId = coupleId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Date deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Date getReadAt() {
        return readAt;
    }

    public void setReadAt(Date readAt) {
        this.readAt = readAt;
    }

    public Integer getRecalledFlag() {
        return recalledFlag;
    }

    public void setRecalledFlag(Integer recalledFlag) {
        this.recalledFlag = recalledFlag;
    }

    public Long getRecalledBy() {
        return recalledBy;
    }

    public void setRecalledBy(Long recalledBy) {
        this.recalledBy = recalledBy;
    }

    public Date getRecalledAt() {
        return recalledAt;
    }

    public void setRecalledAt(Date recalledAt) {
        this.recalledAt = recalledAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
