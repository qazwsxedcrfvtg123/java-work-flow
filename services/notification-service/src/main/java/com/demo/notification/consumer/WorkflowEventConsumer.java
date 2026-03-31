package com.demo.notification.consumer;

import com.demo.common.event.WorkflowCreatedEvent;
import com.demo.common.event.WorkflowApprovedEvent;
import com.demo.common.event.WorkflowRejectedEvent;
import com.demo.common.constant.KafkaTopics;
import com.demo.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class WorkflowEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowEventConsumer.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.WORKFLOW_CREATED, groupId = "notification-group")
    public void consumeWorkflowCreatedEvent(String eventJson) {
        try {
            logger.info("Received workflow created event: {}", eventJson);
            
            // Deserialize the event
            WorkflowCreatedEvent event = objectMapper.readValue(eventJson, WorkflowCreatedEvent.class);
            
            // Process the event and send notification
            notificationService.processWorkflowCreatedEvent(event);
            
            logger.info("Processed workflow created event for workflow ID: {}", event.getWorkflowId());
        } catch (Exception e) {
            logger.error("Error processing workflow created event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.WORKFLOW_APPROVED, groupId = "notification-group")
    public void consumeWorkflowApprovedEvent(String eventJson) {
        try {
            logger.info("Received workflow approved event: {}", eventJson);
            
            WorkflowApprovedEvent event = objectMapper.readValue(eventJson, WorkflowApprovedEvent.class);
            
            // Process the event and send notification
            notificationService.processWorkflowApprovedEvent(event);
            
            logger.info("Processed workflow approved event for workflow ID: {}", event.getWorkflowId());
        } catch (Exception e) {
            logger.error("Error processing workflow approved event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.WORKFLOW_REJECTED, groupId = "notification-group")
    public void consumeWorkflowRejectedEvent(String eventJson) {
        try {
            logger.info("Received workflow rejected event: {}", eventJson);
            
            WorkflowRejectedEvent event = objectMapper.readValue(eventJson, WorkflowRejectedEvent.class);
            
            // Process the event and send notification
            notificationService.processWorkflowRejectedEvent(event);
            
            logger.info("Processed workflow rejected event for workflow ID: {}", event.getWorkflowId());
        } catch (Exception e) {
            logger.error("Error processing workflow rejected event: {}", e.getMessage(), e);
        }
    }
}