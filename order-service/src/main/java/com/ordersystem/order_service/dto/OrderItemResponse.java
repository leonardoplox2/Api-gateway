package com.ordersystem.order_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(UUID productId, Integer quantity, BigDecimal unitPrice) {
}
