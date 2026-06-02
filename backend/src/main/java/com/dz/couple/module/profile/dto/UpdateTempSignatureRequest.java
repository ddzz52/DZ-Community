package com.dz.couple.module.profile.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

public class UpdateTempSignatureRequest {
    @Size(max = 50, message = "临时签名最多50字")
    private String tempSignature;

    @Min(value = 1, message = "有效期仅支持1/3/7天或为空代表永久")
    @Max(value = 7, message = "有效期仅支持1/3/7天或为空代表永久")
    private Integer expireDays;

    public String getTempSignature() {
        return tempSignature;
    }

    public void setTempSignature(String tempSignature) {
        this.tempSignature = tempSignature;
    }

    public Integer getExpireDays() {
        return expireDays;
    }

    public void setExpireDays(Integer expireDays) {
        this.expireDays = expireDays;
    }
}

