package com.dz.couple.module.profile.dto;

import javax.validation.constraints.Size;

public class UpdateMySignatureRequest {
    @Size(max = 50, message = "签名最多50字")
    private String signature;

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}

