package com.demo.workflow.service.impl;

import com.demo.common.dto.CreateWorkflowRequest;
import com.demo.common.dto.WorkflowResponse;
import com.demo.common.event.WorkflowCreatedEvent;
import com.demo.common.event.WorkflowApprovedEvent;
import com.demo.common.event.WorkflowRejectedEvent;
import com.demo.common.constant.KafkaTopics;
import com.demo.common.enums.WorkflowStatus;
import com.demo.workflow.domain.entity.WorkflowRequestEntity;
import com.demo.workflow.domain.entity.WorkflowAuditEntity;
import com.demo.workflow.repository.WorkflowRepository;
import com.demo.workflow.service.WorkflowService;
import com.demo.workflow.event.producer.WorkflowEventProducer;
import com.demo.workflow.validation.WorkflowValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkflowServiceImpl implements WorkflowService {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowEventProducer workflowEventProducer;

    @Autowired
    private WorkflowValidator validator;

    @Override
    public WorkflowResponse createWorkflow(CreateWorkflowRequest request) {
        // Validate the request
        validator.validateCreateRequest(request);

        // Create entity
        WorkflowRequestEntity entity = new WorkflowRequestEntity();
        entity.setWorkflowName(request.getWorkflowName());
        entity.setDescription(request.getDescription());
        entity.setRequester(request.getRequester());
        entity.setPriority(request.getPriority());

        WorkflowRequestEntity savedEntity = workflowRepository.save(entity);

        // Send event to Kafka
        WorkflowCreatedEvent event = new WorkflowCreatedEvent();
        event.setWorkflowId(savedEntity.getId());
        event.setWorkflowName(savedEntity.getWorkflowName());
        event.setDescription(savedEntity.getDescription());
        event.setRequester(savedEntity.getRequester());
        event.setPriority(savedEntity.getPriority());
        event.setTimestamp(LocalDateTime.now());

        workflowEventProducer.sendEvent(KafkaTopics.WORKFLOW_CREATED, event);

        // Convert to response
        WorkflowResponse response = new WorkflowResponse();
        response.setId(savedEntity.getId());
        response.setWorkflowName(savedEntity.getWorkflowName());
        response.setDescription(savedEntity.getDescription());
        response.setRequester(savedEntity.getRequester());
        response.setStatus(savedEntity.getStatus().name());
        response.setCreatedAt(savedEntity.getCreatedAt());
        response.setUpdatedAt(savedEntity.getUpdatedAt());

        return response;
    }

    @Override
    public WorkflowResponse getWorkflowById(Long id) {
        WorkflowRequestEntity entity = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found with id: " + id));

        WorkflowResponse response = new WorkflowResponse();
        response.setId(entity.getId());
        response.setWorkflowName(entity.getWorkflowName());
        response.setDescription(entity.getDescription());
        response.setRequester(entity.getRequester());
        response.setStatus(entity.getStatus().name());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }

    @Override
    public List<WorkflowResponse> getAllWorkflows() {
        List<WorkflowRequestEntity> entities = workflowRepository.findAll();
        return entities.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WorkflowResponse approveWorkflow(Long id, String approver) {
        WorkflowRequestEntity entity = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found with id: " + id));

        if (entity.getStatus() != WorkflowStatus.PENDING) {
            throw new RuntimeException("Workflow is not in pending status");
        }

        entity.setStatus(WorkflowStatus.APPROVED);
        WorkflowRequestEntity updatedEntity = workflowRepository.save(entity);

        // Send approved event
        WorkflowApprovedEvent event = new WorkflowApprovedEvent();
        event.setWorkflowId(updatedEntity.getId());
        event.setApprover(approver);
        event.setTimestamp(LocalDateTime.now());

        workflowEventProducer.sendEvent(KafkaTopics.WORKFLOW_APPROVED, event);

        return convertToResponse(updatedEntity);
    }

    @Override
    public WorkflowResponse rejectWorkflow(Long id, String rejector, String reason) {
        WorkflowRequestEntity entity = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found with id: " + id));

        if (entity.getStatus() != WorkflowStatus.PENDING) {
            throw new RuntimeException("Workflow is not in pending status");
        }

        entity.setStatus(WorkflowStatus.REJECTED);
        WorkflowRequestEntity updatedEntity = workflowRepository.save(entity);

        // Send rejected event
        WorkflowRejectedEvent event = new WorkflowRejectedEvent();
        event.setWorkflowId(updatedEntity.getId());
        event.setRejector(rejector);
        event.setReason(reason);
        event.setTimestamp(LocalDateTime.now());

        workflowEventProducer.sendEvent(KafkaTopics.WORKFLOW_REJECTED, event);

        return convertToResponse(updatedEntity);
    }

    private WorkflowResponse convertToResponse(WorkflowRequestEntity entity) {
        WorkflowResponse response = new WorkflowResponse();
        response.setId(entity.getId());
        response.setWorkflowName(entity.getWorkflowName());
        response.setDescription(entity.getDescription());
        response.setRequester(entity.getRequester());
        response.setStatus(entity.getStatus().name());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }
}