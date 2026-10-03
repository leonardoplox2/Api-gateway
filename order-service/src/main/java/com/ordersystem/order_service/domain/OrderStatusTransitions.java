package com.ordersystem.order_service.domain;

import com.ordersystem.order_service.entity.OrderStatus;

import java.util.EnumMap; 
import java.util.Map;
import java.util.Set;
import java.util.EnumSet;

public final class OrderStatusTransitions {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.FAILED, OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.FAILED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    private OrderStatusTransitions() {
        // Private constructor to prevent instantiation
    }

    public static boolean isValidTransition(OrderStatus from, OrderStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

}
