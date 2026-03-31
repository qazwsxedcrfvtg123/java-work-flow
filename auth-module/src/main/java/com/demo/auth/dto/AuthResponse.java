package com.demo.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;              // Access Token
    private String refreshToken;       // Refresh Token (新增)
    private String username;
    private String role;
    private Long expiresIn;            // Access Token 剩余时间（毫秒）
    private Long refreshExpiresIn;     // Refresh Token 剩余时间（毫秒，新增）
    private String header;
    
    public void setHeader(String key, String value){
        this.header = value;
    }
}
