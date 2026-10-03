package com.ordersystem.order_service.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import java.time.Instant;
import java.util.UUID;



@Entity 
@Table(name = "order_status_history")
@Getter 
@NoArgsConstructor 
public class OrderStatusHistory {

    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private  UUID   id;

    @Enumerated(EnumType.STRING)
    @Column (nullable = false)
    private OrderStatus previousStatus;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false)
    private OrderStatus newStatus;

    private String reason;

    @Column (nullable = false, updatable = false)
    private Instant changedAt;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    public static OrderStatusHistory of(Order order, OrderStatus previousStatus, OrderStatus newStatus, String reason) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.order = order;
        history.previousStatus = previousStatus;
        history.newStatus = newStatus;
        history.reason = reason;
        history.changedAt = Instant.now();
        return history;
    }

}
