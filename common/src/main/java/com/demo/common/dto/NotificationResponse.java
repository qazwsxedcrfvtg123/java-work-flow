package com.demo.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private String recipient;
    private String message;
    private String type; // EMAIL, SMS, PUSH
    private String status; // SENT, FAILED
    private LocalDateTime sentAt;
}