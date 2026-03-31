package com.demo.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRejectedEvent extends BaseEvent {
    private Long workflowId;
    private String rejector;
    private String reason;
    private LocalDateTime timestamp;
}