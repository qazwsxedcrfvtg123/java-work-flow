package com.demo.workflow.validation;

import com.demo.common.dto.CreateWorkflowRequest;
import com.demo.common.constant.ErrorCodes;
import org.springframework.stereotype.Component;

@Component
public class WorkflowValidator {

    public void validateCreateRequest(CreateWorkflowRequest request) {
        if (request.getWorkflowName() == null || request.getWorkflowName().trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow name is required");
        }
        
        if (request.getRequester() == null || request.getRequester().trim().isEmpty()) {
            throw new IllegalArgumentException("Requester is required");
        }
        
        if (!isValidPriority(request.getPriority())) {
            throw new IllegalArgumentException("Invalid priority. Must be LOW, MEDIUM, or HIGH");
        }
    }
    
    private boolean isValidPriority(String priority) {
        if (priority == null) return false;
        return priority.equalsIgnoreCase("LOW") || 
               priority.equalsIgnoreCase("MEDIUM") || 
               priority.equalsIgnoreCase("HIGH");
    }
}