package com.demo.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "auth.jwt")
public class AuthProperties {
    private String secret = "defaultSecretKeyForDevelopmentOnly";
    private Long expiration = 86400000L;
    private Long refreshExpiration = 604800000L;
}
