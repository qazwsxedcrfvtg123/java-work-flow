package com.demo.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkflowResponse {
    private Long id;
    private String workflowName;
    private String description;
    private String requester;
    private String status; // PENDING, APPROVED, REJECTED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}