package com.ordersystem.order_service.error;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String traceId
) {
    public static ErrorResponse of(HttpStatus status, String code, String message, String traceId) {
        return new ErrorResponse(Instant.now(), status.value(), code, message, traceId);
    }
}
