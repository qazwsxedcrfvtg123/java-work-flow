package com.demo.auth.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.demo.auth.config.R2Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
* @description:
 * @Creator: 阿昇
 * @CreateTime: 2026-03-30 19:57
 */

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(R2Properties.class)
public class R2Config {
   private final R2Properties r2Properties;

   @Bean public AmazonS3 s3Client() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(r2Properties.getAccessKey(), r2Properties.getSecretKey());

        return AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            // 關鍵：將 Endpoint 指向 Cloudflare R2
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                r2Properties.getEndpoint(),
                r2Properties.getRegion()))
            // 建議開啟，R2 支援 Path Style 存取
            .withPathStyleAccessEnabled(true)
            .build();
    }
}
