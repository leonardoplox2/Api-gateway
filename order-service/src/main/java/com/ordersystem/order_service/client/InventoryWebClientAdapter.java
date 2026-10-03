package com.ordersystem.order_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.ordersystem.order_service.client.dto.ReserveStockClientRequest;
import reactor.core.publisher.Mono;
import java.util.UUID;
import java.time.Duration;
import java.util.concurrent.TimeoutException;


@Component 
public class InventoryWebClientAdapter implements InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryWebClientAdapter.class);

    private final WebClient inventoryWebClient;

    public InventoryWebClientAdapter(WebClient inventoryWebClient) {
        this.inventoryWebClient = inventoryWebClient;
    }

    @Override
    public Mono<StockReservationResult> reserveStock(UUID productId, int quantity, String traceId) {
        return inventoryWebClient.post()
                .uri("/api/inventory/reserve")
                .header("X-Trace-Id", traceId)
                .bodyValue(new ReserveStockClientRequest(productId, quantity))
                .exchangeToMono(response ->{
                    if(response.statusCode().is2xxSuccessful()){
                        return Mono.<StockReservationResult>just(new StockReservationResult.Reserved());
                    }
                    if(response.statusCode().value() == 409){
                        return Mono.<StockReservationResult>just(new StockReservationResult.InsufficientStock("Stock insufficient para producto: " + productId));  
                    }
                    if(response.statusCode().value() == 404){
                        return Mono.<StockReservationResult>just(new StockReservationResult.ProductNotFound("Producto no encontrado: " + productId));
                    }
                    return response.createException().flatMap(Mono::error);
                })
                .timeout(Duration.ofSeconds(3))
                .onErrorMap(TimeoutException.class, ex -> new IllegalStateException("Inventory Service no respondio a tiempo", ex));
    }

    @Override
    public Mono<Void> releaseStock(UUID productId, int quantity, String traceId) {
        return inventoryWebClient.post()
                .uri("/api/inventory/release")
                .header("X-Trace-Id", traceId)
                .bodyValue(new ReserveStockClientRequest(productId, quantity))
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(ex -> {
                    log.error("No se pudo liberar stock del producto {} (cantidad {}): {}",
                            productId, quantity, ex.getMessage());
                    return Mono.empty();
                });
    }

}
