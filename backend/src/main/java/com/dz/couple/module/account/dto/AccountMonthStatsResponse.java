package com.dz.couple.module.account.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class AccountMonthStatsResponse {
    private Integer year;
    private Integer month;
    private Date from;
    private Date to;
    private BigDecimal totalAmount;
    private BigDecimal monthlyBudget;
    private Boolean overBudget;
    private BigDecimal budgetRemaining;
    private List<CategoryItem> byCategory;
    private List<UserItem> byUser;

    public static class CategoryItem {
        private String category;
        private BigDecimal amount;

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public static class UserItem {
        private Long userId;
        private BigDecimal amount;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Date getFrom() {
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public Date getTo() {
        return to;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<CategoryItem> getByCategory() {
        return byCategory;
    }

    public void setByCategory(List<CategoryItem> byCategory) {
        this.byCategory = byCategory;
    }

    public List<UserItem> getByUser() {
        return byUser;
    }

    public void setByUser(List<UserItem> byUser) {
        this.byUser = byUser;
    }

    public BigDecimal getMonthlyBudget() { return monthlyBudget; }
    public void setMonthlyBudget(BigDecimal monthlyBudget) { this.monthlyBudget = monthlyBudget; }
    public Boolean getOverBudget() { return overBudget; }
    public void setOverBudget(Boolean overBudget) { this.overBudget = overBudget; }
    public BigDecimal getBudgetRemaining() { return budgetRemaining; }
    public void setBudgetRemaining(BigDecimal budgetRemaining) { this.budgetRemaining = budgetRemaining; }
}

