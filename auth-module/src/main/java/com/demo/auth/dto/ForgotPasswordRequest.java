package com.demo.auth.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * @description:
 * @Creator: 阿昇
 * @CreateTime: 2026-10-15 20:00
 */
@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "信箱不可為空")
    @Email(message = "信箱格式錯誤")
    private String email;
}
