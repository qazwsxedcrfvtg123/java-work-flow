package com.demo.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auth Module Spring Boot Application
 * Can be run as a standalone service or included as a library
 */
@SpringBootApplication
@EntityScan(basePackages = "com.demo.auth.entity")
@EnableJpaRepositories(basePackages = "com.demo.auth.repository")
public class AuthModuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthModuleApplication.class, args);
    }
}
