package com.demo.common.dto;

import lombok.Data;

@Data
public class CreateWorkflowRequest {
    private String workflowName;
    private String description;
    private String requester;
    private String priority; // LOW, MEDIUM, HIGH
}