package com.ordersystem.order_service.client;

import java.util.UUID;
import  reactor.core.publisher.Mono;

public interface InventoryClient {
    Mono<StockReservationResult> reserveStock(UUID productId, int quantity, String traceId);

    Mono<Void> releaseStock(UUID productId, int quantity, String traceId);

}
