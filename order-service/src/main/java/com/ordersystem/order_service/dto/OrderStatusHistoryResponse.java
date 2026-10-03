package com.ordersystem.order_service.dto;

import com.ordersystem.order_service.entity.OrderStatus;

import java.time.Instant;

public record OrderStatusHistoryResponse(
        OrderStatus previousStatus,
        OrderStatus newStatus,
        String reason,
        Instant changedAt
) {
}
