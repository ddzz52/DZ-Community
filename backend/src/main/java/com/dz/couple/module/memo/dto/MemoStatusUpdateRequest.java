package com.dz.couple.module.memo.dto;

import javax.validation.constraints.NotNull;

public class MemoStatusUpdateRequest {
    @NotNull(message = "请选择状态")
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
