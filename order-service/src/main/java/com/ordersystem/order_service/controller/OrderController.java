package com.ordersystem.order_service.controller;

import com.ordersystem.order_service.dto.CreateOrderRequest;
import com.ordersystem.order_service.dto.OrderResponse;
import com.ordersystem.order_service.dto.OrderStatusHistoryResponse;
import com.ordersystem.order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(
            summary = "Crear un nuevo pedido",
            description = "Registra un pedido con sus items y reserva el stock correspondiente en Inventory Service. "
                    + "El pedido queda en CONFIRMED si hay stock disponible para todos los items, o en FAILED si no.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido creado (CONFIRMED o FAILED segun disponibilidad de stock)"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos en la solicitud (items vacios, cantidad negativa, etc.)"),
            @ApiResponse(responseCode = "401", description = "Token invalido, expirado o no proporcionado")
    })
    @PostMapping
    public Mono<ResponseEntity<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            ServerHttpRequest httpRequest) {
        String traceId = httpRequest.getHeaders().getFirst("X-Trace-Id");
        return orderService.createOrder(request, traceId)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(summary = "Consultar un pedido por su id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un pedido con ese id")
    })
    @GetMapping("/{id}")
    public Mono<OrderResponse> getOrder(@PathVariable UUID id) {
        return orderService.getOrder(id);
    }

    @Operation(
            summary = "Cancelar un pedido",
            description = "Solo permite cancelar pedidos en estado PENDING o CONFIRMED. "
                    + "No permite cancelar un pedido que ya esta CANCELLED o FAILED (transicion invalida).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido cancelado"),
            @ApiResponse(responseCode = "404", description = "No existe un pedido con ese id"),
            @ApiResponse(responseCode = "409", description = "Transicion invalida (ej. cancelar un pedido ya cancelado)")
    })
    @PostMapping("/{id}/cancel")
    public Mono<OrderResponse> cancelOrder(@PathVariable UUID id, ServerHttpRequest httpRequest) {
        String traceId = httpRequest.getHeaders().getFirst("X-Trace-Id");
        return orderService.cancelOrder(id, traceId);
    }

    @Operation(summary = "Consultar el historial de cambios de estado de un pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un pedido con ese id")
    })
    @GetMapping("/{id}/history")
    public Mono<List<OrderStatusHistoryResponse>> getHistory(@PathVariable UUID id) {
        return orderService.getHistory(id);
    }
}
