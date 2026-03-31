package com.demo.workflow.domain.entity;

import com.demo.common.enums.WorkflowStatus;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_requests")
@Data
public class WorkflowRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_name", nullable = false)
    private String workflowName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "requester", nullable = false)
    private String requester;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('PENDING', 'APPROVED', 'REJECTED')")
    private WorkflowStatus status;

    @Column(name = "priority", nullable = false)
    private String priority;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = WorkflowStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
