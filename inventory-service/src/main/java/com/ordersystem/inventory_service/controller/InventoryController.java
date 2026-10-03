package com.ordersystem.inventory_service.controller;

import com.ordersystem.inventory_service.dto.ReleaseStockRequest;
import com.ordersystem.inventory_service.dto.ReserveStockRequest;
import com.ordersystem.inventory_service.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Operation(
            summary = "Reservar stock de un producto",
            description = "Descuenta la cantidad solicitada del stock disponible del producto, si existe suficiente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock reservado correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe un producto con ese id"),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente para la cantidad solicitada")
    })
    @PostMapping("/reserve")
    public Mono<ResponseEntity<Void>> reserveStock(@Valid @RequestBody ReserveStockRequest request) {
        return Mono.fromCallable(() -> {
                    inventoryService.reserveStock(request.productId(), request.quantity());
                    return ResponseEntity.ok().<Void>build();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(
            summary = "Liberar stock de un producto",
            description = "Devuelve al stock disponible la cantidad indicada. Se usa cuando se cancela "
                    + "un pedido que ya estaba CONFIRMED (que ya habia reservado stock real).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock liberado correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe un producto con ese id")
    })
    @PostMapping("/release")
    public Mono<ResponseEntity<Void>> releaseStock(@Valid @RequestBody ReleaseStockRequest request) {
        return Mono.fromCallable(() -> {
                    inventoryService.releaseStock(request.productId(), request.quantity());
                    return ResponseEntity.ok().<Void>build();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}