package com.ordersystem.inventory_service.error;
import java.time.Instant;
import org.springframework.http.HttpStatus;

public record ErrorResponse (
    Instant timestamp,
    int status,
    String code,
    String message,
    String traceId
){
    public static ErrorResponse of(HttpStatus status, String code, String message, String traceId) {
        return new ErrorResponse(Instant.now(), status.value(), code, message, traceId);
    }

}
