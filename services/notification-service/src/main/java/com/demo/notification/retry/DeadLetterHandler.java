package com.demo.notification.retry;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.ErrorHandler;
import org.springframework.stereotype.Component;

@Component
public class DeadLetterHandler implements ErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterHandler.class);

    @Override
    public void handle(Exception thrownException, ConsumerRecord<?, ?> data) {
        logger.error("Processing failed for consumer record: {}, error: {}", data, thrownException.getMessage(), thrownException);
        // In a real implementation, we would send the message to a dead letter topic
        // For now, we just log the error
    }
}