package com.dz.couple.module.timeline.dto;

import java.util.Date;

public class TimelineItemVO {
    private String type;
    private Integer typeRank;
    private Long refId;
    private Date eventAt;
    private String title;
    private String content;
    private String thumbUrl;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getTypeRank() {
        return typeRank;
    }

    public void setTypeRank(Integer typeRank) {
        this.typeRank = typeRank;
    }

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
    }

    public Date getEventAt() {
        return eventAt;
    }

    public void setEventAt(Date eventAt) {
        this.eventAt = eventAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public void setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }
}
