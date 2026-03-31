package com.demo.notification.config;

import com.demo.common.constant.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred in notification service: ", ex);
        ErrorResponse error = new ErrorResponse(
                "NOTIFICATION_INTERNAL_ERROR",
                "An unexpected error occurred in notification service",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime error occurred in notification service: ", ex);
        ErrorResponse error = new ErrorResponse(
                ErrorCodes.NOTIFICATION_FAILED,
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    // Error response class
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private int status;
        private LocalDateTime timestamp;

        public ErrorResponse(String errorCode, String message, int status, LocalDateTime timestamp) {
            this.errorCode = errorCode;
            this.message = message;
            this.status = status;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}