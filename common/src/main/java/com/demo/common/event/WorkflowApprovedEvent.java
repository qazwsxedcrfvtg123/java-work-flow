package com.demo.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowApprovedEvent extends BaseEvent {
    private Long workflowId;
    private String approver;
    private LocalDateTime timestamp;
}