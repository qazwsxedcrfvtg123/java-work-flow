package com.demo.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(R2Properties.class)
@ConfigurationProperties(prefix = "aws.s3")
@Data // Lombok
public class R2Properties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String region;
    private String bucketName;
}