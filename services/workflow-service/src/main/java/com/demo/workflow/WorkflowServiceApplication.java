package com.demo.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.demo.workflow", "com.demo.auth"})
@EntityScan(basePackages = {"com.demo.workflow.domain", "com.demo.auth.domain.entity"})
@EnableJpaRepositories(basePackages = {"com.demo.workflow.repository"})
public class WorkflowServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkflowServiceApplication.class, args);
    }
}