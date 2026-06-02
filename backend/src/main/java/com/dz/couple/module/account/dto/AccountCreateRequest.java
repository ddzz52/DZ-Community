package com.dz.couple.module.account.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class AccountCreateRequest {
    @NotBlank(message = "请选择分类")
    @Size(max = 32, message = "分类长度需≤32")
    private String category;

    @NotNull(message = "请输入金额")
    @DecimalMin(value = "0.01", message = "金额需≥0.01")
    private BigDecimal amount;

    private String occurredAt;

    @Size(max = 200, message = "备注长度需≤200")
    private String remark;

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

    public String getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(String occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

