package com.demo.workflow.event.mapper;

import com.demo.common.event.BaseEvent;
import com.demo.workflow.domain.entity.WorkflowRequestEntity;
import com.demo.workflow.domain.entity.WorkflowAuditEntity;

public class WorkflowEventMapper {
    
    public static WorkflowAuditEntity mapToAuditEntity(BaseEvent event, String action, String actor, String reason) {
        WorkflowAuditEntity audit = new WorkflowAuditEntity();
        audit.setAction(action);
        audit.setActor(actor);
        audit.setReason(reason);
        // Extract workflow ID from event if possible
        return audit;
    }
}