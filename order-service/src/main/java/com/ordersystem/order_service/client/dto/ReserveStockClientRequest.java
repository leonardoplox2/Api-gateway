package com.ordersystem.order_service.client.dto;

import java.util.UUID;

public record ReserveStockClientRequest(UUID productId, Integer quantity) {


}
