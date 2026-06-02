package com.dz.couple.module.account.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class AccountYearStatsResponse {
    private Integer year;
    private Date from;
    private Date to;
    private BigDecimal totalAmount;
    private List<MonthItem> byMonth;
    private List<AccountMonthStatsResponse.CategoryItem> byCategory;

    public static class MonthItem {
        private Integer month;
        private BigDecimal amount;

        public Integer getMonth() {
            return month;
        }

        public void setMonth(Integer month) {
            this.month = month;
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

    public List<MonthItem> getByMonth() {
        return byMonth;
    }

    public void setByMonth(List<MonthItem> byMonth) {
        this.byMonth = byMonth;
    }

    public List<AccountMonthStatsResponse.CategoryItem> getByCategory() {
        return byCategory;
    }

    public void setByCategory(List<AccountMonthStatsResponse.CategoryItem> byCategory) {
        this.byCategory = byCategory;
    }
}

