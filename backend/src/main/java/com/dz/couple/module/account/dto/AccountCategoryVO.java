package com.dz.couple.module.account.dto;

import java.util.Date;

public class AccountCategoryVO {
    private String category;
    private Integer count;
    private Date lastAt;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Date getLastAt() {
        return lastAt;
    }

    public void setLastAt(Date lastAt) {
        this.lastAt = lastAt;
    }
}

