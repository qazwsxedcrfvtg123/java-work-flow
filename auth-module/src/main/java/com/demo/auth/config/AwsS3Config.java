package com.demo.auth.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AWS S3 Configuration
 */
//@Configuration
//public class AwsS3Config {
//
//    @Value("${amazon.s3.access.key}")
//    private String accessKey;
//
//    @Value("${amazon.s3.secret.key}")
//    private String secretKey;
//
//    @Value("${oss.client.endpoint:https://s3.amazonaws.com}")
//    private String endpoint;
//
//    @Bean
//    public AmazonS3 amazonS3() {
//        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
//        return AmazonS3ClientBuilder
//                .standard()
//                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
//                .withEndpointConfiguration(new com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration(
//                        endpoint, "ap-southeast-2")) // Australia (Sydney) region
//                .build();
//    }
//}
