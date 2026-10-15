package com.demo.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @description:
 * @Creator: 阿昇
 * @CreateTime: 2026-10-15 20:01
 */
@Data
public class VerifyCodeRequest {
    @NotBlank
    private String email;
    @NotBlank private String code;
}
