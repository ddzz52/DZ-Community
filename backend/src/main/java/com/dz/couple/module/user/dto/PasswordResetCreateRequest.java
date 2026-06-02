package com.dz.couple.module.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class PasswordResetCreateRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 32, message = "用户名过长")
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
