package com.ordersystem.order_service.entity;

import com.ordersystem.order_service.domain.OrderStatusTransitions;
import com.ordersystem.order_service.exception.InvalidOrderTransitionException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    public static Order createNew() {
        Order order = new Order();
        order.status = OrderStatus.PENDING;
        order.createdAt = Instant.now();
        return order;
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
    }

    public void confirm() {
        transitionTo(OrderStatus.CONFIRMED, "Stock reservado correctamente");
    }

    public void fail(String reason) {
        transitionTo(OrderStatus.FAILED, reason);
    }

    public void cancel() {
        transitionTo(OrderStatus.CANCELLED, "Cancelado por el usuario");
    }

    private void transitionTo(OrderStatus newStatus, String reason) {
        if (!OrderStatusTransitions.isValidTransition(this.status, newStatus)) {
            throw new InvalidOrderTransitionException(
                    "No se puede pasar de " + this.status + " a " + newStatus);
        }
        OrderStatus previousStatus = this.status;
        this.status = newStatus;
        this.statusHistory.add(OrderStatusHistory.of(this, previousStatus, newStatus, reason));
    }
}