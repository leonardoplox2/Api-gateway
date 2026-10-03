package com.ordersystem.inventory_service.error;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.ordersystem.inventory_service.exception.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import com.ordersystem.inventory_service.exception.InsufficientStockException;

@RestControllerAdvice
public class GlobalExceptionHandler {

   @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex, ServerHttpRequest request) {
        String traceId = request.getHeaders().getFirst("X-Trace-Id");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", ex.getMessage(), traceId));
    }

    @ExceptionHandler (InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex, ServerHttpRequest request) {
        String traceId = request.getHeaders().getFirst("X-Trace-Id");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT, "STOCK_INSUFFICIENT", ex.getMessage(), traceId));
    }
}
