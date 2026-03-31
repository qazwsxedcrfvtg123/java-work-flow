package com.demo.notification.service;

import com.demo.common.dto.NotificationResponse;
import com.demo.common.event.WorkflowCreatedEvent;
import com.demo.common.event.WorkflowApprovedEvent;
import com.demo.common.event.WorkflowRejectedEvent;

import java.util.List;

public interface NotificationService {
    NotificationResponse getNotificationById(Long id);
    List<NotificationResponse> getAllNotifications(int page, int size);
    void processWorkflowCreatedEvent(WorkflowCreatedEvent event);
    void processWorkflowApprovedEvent(WorkflowApprovedEvent event);
    void processWorkflowRejectedEvent(WorkflowRejectedEvent event);
}