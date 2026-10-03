package com.ordersystem.order_service.client;

public sealed interface StockReservationResult { 

    record Reserved() implements StockReservationResult {}
    record InsufficientStock(String message) implements StockReservationResult {}
    record ProductNotFound(String message) implements StockReservationResult {}

}
