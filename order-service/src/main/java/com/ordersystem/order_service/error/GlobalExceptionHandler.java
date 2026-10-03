package com.ordersystem.order_service.error;

import com.ordersystem.order_service.exception.InvalidOrderTransitionException;
import com.ordersystem.order_service.exception.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex, ServerHttpRequest request) {
        String traceId = request.getHeaders().getFirst("X-Trace-Id");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", ex.getMessage(), traceId));
    }

    @ExceptionHandler(InvalidOrderTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidOrderTransitionException ex, ServerHttpRequest request) {
        String traceId = request.getHeaders().getFirst("X-Trace-Id");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT, "INVALID_TRANSITION", ex.getMessage(), traceId));
    }
}
