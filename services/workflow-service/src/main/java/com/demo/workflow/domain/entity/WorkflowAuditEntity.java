package com.demo.workflow.domain.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_audits")
@Data
public class WorkflowAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_request_id", nullable = false)
    private Long workflowRequestId;

    @Column(name = "action", nullable = false)
    private String action; // APPROVED, REJECTED

    @Column(name = "actor", nullable = false)
    private String actor;

    @Column(name = "reason")
    private String reason;

    @Column(name = "performed_at", nullable = false, updatable = false)
    private LocalDateTime performedAt;

    @PrePersist
    protected void onCreate() {
        performedAt = LocalDateTime.now();
    }
}