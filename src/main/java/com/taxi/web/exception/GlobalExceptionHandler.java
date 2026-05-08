package com.taxi.web.exception;

import com.taxi.domain.tenant.exception.TenantConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TenantConfigurationException.class)
    public ResponseEntity<Map<String, Object>> handleTenantConfigurationException(TenantConfigurationException ex) {
        log.warn("TenantConfigurationException for tenant '{}': {}", ex.getTenantId(), ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "Configuration Required");
        body.put("message", ex.getMessage());
        body.put("configKey", ex.getConfigKey());
        return new ResponseEntity<>(body, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("IllegalStateException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());

        // Provide user-friendly error message for date parsing errors
        String message = ex.getMessage();
        if (message != null && message.contains("Parse attempt failed")) {
            message = "Invalid date format. Please ensure all date parameters are provided in YYYY-MM-DD format.";
        }

        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Catch-all exception handler to ensure ALL exceptions return JSON instead of HTML error pages.
     * This prevents "Unexpected token '<', "<html>..." errors on the frontend.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        // Provide user-friendly messages for common error types
        String message = ex.getMessage();
        if (message == null || message.isEmpty()) {
            message = "An unexpected error occurred while processing your request";
        }

        // Handle specific error patterns with user-friendly messages
        if (message.contains("OutOfMemoryError") || message.contains("memory")) {
            message = "File too large to process. Please try a smaller file or contact support.";
        } else if (message.contains("timeout") || message.contains("Timeout")) {
            message = "Request timed out. The file may be too complex. Please try again or contact support.";
        } else if (message.contains("PDFBox") || message.contains("PDF")) {
            message = "Unable to process PDF file. The file may be corrupted or password-protected.";
        }

        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}
