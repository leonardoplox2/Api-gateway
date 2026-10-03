package com.ordersystem.order_service.domain;

import com.ordersystem.order_service.entity.OrderStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderStatusTransitionsTest {

    @ParameterizedTest(name = "{0} -> {1} deberia ser {2}")
    @MethodSource("transiciones")
    void validaTransiciones(OrderStatus from, OrderStatus to, boolean esperado) {
        assertEquals(esperado, OrderStatusTransitions.isValidTransition(from, to));
    }

    static Stream<Arguments> transiciones() {
        return Stream.of(
                Arguments.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, true),
                Arguments.of(OrderStatus.PENDING, OrderStatus.FAILED, true),
                Arguments.of(OrderStatus.PENDING, OrderStatus.CANCELLED, true),
                Arguments.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, true),
                Arguments.of(OrderStatus.CONFIRMED, OrderStatus.PENDING, false),
                Arguments.of(OrderStatus.CONFIRMED, OrderStatus.FAILED, false),
                Arguments.of(OrderStatus.FAILED, OrderStatus.PENDING, false),
                Arguments.of(OrderStatus.FAILED, OrderStatus.CONFIRMED, false),
                Arguments.of(OrderStatus.FAILED, OrderStatus.CANCELLED, false),
                Arguments.of(OrderStatus.CANCELLED, OrderStatus.PENDING, false),
                Arguments.of(OrderStatus.CANCELLED, OrderStatus.CONFIRMED, false),
                Arguments.of(OrderStatus.CANCELLED, OrderStatus.FAILED, false)
        );
    }
}
