package com.ordersystem.order_service.dto;

import com.ordersystem.order_service.entity.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderStatus status,
        Instant createdAt,
        List<OrderItemResponse> items
) {
}
