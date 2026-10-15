package com.demo.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * @description:
 * @Creator: 阿昇
 * @CreateTime: @Data10-15 20:01
 */
@Data
public class ResetPasswordRequest {
    @NotBlank
    private String email;     // 或改用 token 帶過來
    @NotBlank private String code;
    @NotBlank
    @Size(min = 6, max = 32, message = "新密碼長度需為 6~32 位")
    private String newPassword;
}
