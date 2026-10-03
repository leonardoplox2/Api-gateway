package com.ordersystem.order_service.service;

import com.ordersystem.order_service.client.InventoryClient;
import com.ordersystem.order_service.client.StockReservationResult;
import com.ordersystem.order_service.dto.CreateOrderRequest;
import com.ordersystem.order_service.dto.OrderItemResponse;
import com.ordersystem.order_service.dto.OrderResponse;
import com.ordersystem.order_service.dto.OrderStatusHistoryResponse;
import com.ordersystem.order_service.entity.Order;
import com.ordersystem.order_service.entity.OrderItem;
import com.ordersystem.order_service.entity.OrderStatus;
import com.ordersystem.order_service.exception.OrderNotFoundException;
import com.ordersystem.order_service.repository.OrderRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    public OrderService(OrderRepository orderRepository, InventoryClient inventoryClient) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
    }

    public Mono<OrderResponse> createOrder(CreateOrderRequest request, String traceId) {
        Order order = Order.createNew();
        request.items().forEach(itemReq -> order.addItem(
                OrderItem.builder()
                        .productId(itemReq.productId())
                        .quantity(itemReq.quantity())
                        .unitPrice(itemReq.unitPrice())
                        .build()
        ));

        return saveBlocking(order)
                .flatMap(savedOrder -> reserveAllItems(savedOrder, traceId)
                        .map(results -> applyReservationResults(savedOrder, results)))
                .flatMap(this::saveBlocking)
                .map(this::toResponse);
    }

    public Mono<OrderResponse> getOrder(UUID id) {
        return findOrder(id).map(this::toResponse);
    }

    public Mono<OrderResponse> cancelOrder(UUID id, String traceId) {
        return findOrder(id)
                .flatMap(order -> {
                    OrderStatus previousStatus = order.getStatus();
                    order.cancel();
                    return saveBlocking(order)
                            .flatMap(savedOrder -> {
                                if (previousStatus == OrderStatus.CONFIRMED) {
                                    return releaseAllItems(savedOrder, traceId).thenReturn(savedOrder);
                                }
                                return Mono.just(savedOrder);
                            });
                })
                .map(this::toResponse);
    }

    public Mono<List<OrderStatusHistoryResponse>> getHistory(UUID id) {
        return findOrder(id)
                .map(order -> order.getStatusHistory().stream()
                        .map(h -> new OrderStatusHistoryResponse(
                                h.getPreviousStatus(), h.getNewStatus(), h.getReason(), h.getChangedAt()))
                        .toList());
    }

    private Mono<List<StockReservationResult>> reserveAllItems(Order order, String traceId) {
        return Flux.fromIterable(order.getItems())
                .concatMap(item -> inventoryClient.reserveStock(item.getProductId(), item.getQuantity(), traceId))
                .collectList();
    }

    private Order applyReservationResults(Order order, List<StockReservationResult> results) {
        String failureReason = null;
        for (StockReservationResult result : results) {
            if (!(result instanceof StockReservationResult.Reserved)) {
                failureReason = describeFailure(result);
                break;
            }
        }

        if (failureReason == null) {
            order.confirm();
        } else {
            order.fail(failureReason);
        }
        return order;
    }

    private Mono<Void> releaseAllItems(Order order, String traceId) {
        return Flux.fromIterable(order.getItems())
                .concatMap(item -> inventoryClient.releaseStock(item.getProductId(), item.getQuantity(), traceId))
                .then();
    }

    private String describeFailure(StockReservationResult result) {
        if (result instanceof StockReservationResult.InsufficientStock insufficientStock) {
            return insufficientStock.message();
        }
        if (result instanceof StockReservationResult.ProductNotFound productNotFound) {
            return productNotFound.message();
        }
        return "Error desconocido al reservar stock";
    }

    private Mono<Order> findOrder(UUID id) {
        return Mono.fromCallable(() -> orderRepository.findById(id)
                        .orElseThrow(() -> new OrderNotFoundException("Pedido no encontrado: " + id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Order> saveBlocking(Order order) {
        return Mono.fromCallable(() -> orderRepository.save(order))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new OrderResponse(order.getId(), order.getStatus(), order.getCreatedAt(), items);
    }
}
