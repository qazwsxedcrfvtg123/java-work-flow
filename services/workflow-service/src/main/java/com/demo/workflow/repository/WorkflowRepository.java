package com.demo.workflow.repository;

import com.demo.workflow.domain.entity.WorkflowRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowRequestEntity, Long> {
}