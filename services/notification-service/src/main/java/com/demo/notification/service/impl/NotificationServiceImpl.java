package com.demo.notification.service.impl;

import com.demo.common.dto.NotificationResponse;
import com.demo.common.event.WorkflowCreatedEvent;
import com.demo.common.event.WorkflowApprovedEvent;
import com.demo.common.event.WorkflowRejectedEvent;
import com.demo.notification.domain.entity.NotificationEntity;
import com.demo.notification.domain.enums.NotificationType;
import com.demo.notification.repository.NotificationRepository;
import com.demo.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public NotificationResponse getNotificationById(Long id) {
        NotificationEntity entity = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));

        return convertToResponse(entity);
    }

    @Override
    public List<NotificationResponse> getAllNotifications(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<NotificationEntity> entities = notificationRepository.findAll(pageable).getContent();
        return entities.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void processWorkflowCreatedEvent(WorkflowCreatedEvent event) {
        NotificationEntity notification = new NotificationEntity();
        notification.setRecipient(event.getRequester());
        notification.setMessage(String.format("Your workflow '%s' has been submitted and is pending approval.", event.getWorkflowName()));
        notification.setType(NotificationType.EMAIL); // Default to email
        notification.setStatus("SENT");

        notificationRepository.save(notification);
    }

    @Override
    public void processWorkflowApprovedEvent(WorkflowApprovedEvent event) {
        // In a real application, we would need to retrieve the requester from the workflow
        // For now, we'll use a placeholder
        NotificationEntity notification = new NotificationEntity();
        notification.setRecipient("requester@example.com"); // Placeholder
        notification.setMessage(String.format("Your workflow has been approved by %s.", event.getApprover()));
        notification.setType(NotificationType.EMAIL);
        notification.setStatus("SENT");

        notificationRepository.save(notification);
    }

    @Override
    public void processWorkflowRejectedEvent(WorkflowRejectedEvent event) {
        NotificationEntity notification = new NotificationEntity();
        notification.setRecipient("requester@example.com"); // Placeholder
        notification.setMessage(String.format("Your workflow has been rejected by %s. Reason: %s", 
                event.getRejector(), event.getReason() != null ? event.getReason() : "No reason provided"));
        notification.setType(NotificationType.EMAIL);
        notification.setStatus("SENT");

        notificationRepository.save(notification);
    }

    private NotificationResponse convertToResponse(NotificationEntity entity) {
        NotificationResponse response = new NotificationResponse();
        response.setId(entity.getId());
        response.setRecipient(entity.getRecipient());
        response.setMessage(entity.getMessage());
        response.setType(entity.getType().name());
        response.setStatus(entity.getStatus());
        response.setSentAt(entity.getSentAt());
        return response;
    }
}