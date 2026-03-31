package com.demo.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowCreatedEvent extends BaseEvent {
    private Long workflowId;
    private String workflowName;
    private String description;
    private String requester;
    private String priority;
    private LocalDateTime timestamp;
}