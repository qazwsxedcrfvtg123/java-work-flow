package com.demo.workflow.service;

import com.demo.common.dto.CreateWorkflowRequest;
import com.demo.common.dto.WorkflowResponse;

import java.util.List;

public interface WorkflowService {
    WorkflowResponse createWorkflow(CreateWorkflowRequest request);
    WorkflowResponse getWorkflowById(Long id);
    List<WorkflowResponse> getAllWorkflows();
    WorkflowResponse approveWorkflow(Long id, String approver);
    WorkflowResponse rejectWorkflow(Long id, String rejector, String reason);
}