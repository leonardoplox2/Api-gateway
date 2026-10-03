package com.ordersystem.order_service.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.ordersystem.order_service.exception.InvalidOrderTransitionException;

class OrderTest {

    @Test 
    void unPedidoNuevoEmpienzaEnPending(){
        Order order = Order.createNew();
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void confirmarUnPedidoteLoDejaConfirmado(){
        Order order = Order.createNew();
        order.confirm();
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void confirmarUnPedidoRegistraHistorial(){
        Order order = Order.createNew();
        order.confirm();
        assertEquals(1, order.getStatusHistory().size());
        assertEquals(OrderStatus.PENDING, order.getStatusHistory().get(0).getPreviousStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getStatusHistory().get(0).getNewStatus());
    }

    @Test 
    void cancelarUnPedidoYaCanceladoLanzaExcepcion(){
        Order order = Order.createNew();
        order.cancel();
        assertThrows(InvalidOrderTransitionException.class, order::cancel);
    }

    @Test 
    void confirmarUnPedidoYaConfirmadoLanzaExcepcion(){
        Order order = Order.createNew();
        order.confirm();
        assertThrows(InvalidOrderTransitionException.class, order::confirm);

    }

    @Test 
    void cancelarUnPedidoYaConfirmado(){
        Order order = Order.createNew();
        order.confirm();
        order.cancel();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

}
