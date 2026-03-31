package com.demo.workflow.event.producer;

import com.demo.common.event.BaseEvent;
import com.demo.workflow.config.KafkaProducerConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class WorkflowEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowEventProducer.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public void sendEvent(String topic, BaseEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, eventJson);
            logger.info("Event sent to topic '{}': {}", topic, eventJson);
        } catch (Exception e) {
            logger.error("Error sending event to topic '{}': {}", topic, e.getMessage(), e);
            throw new RuntimeException("Failed to send event", e);
        }
    }
}