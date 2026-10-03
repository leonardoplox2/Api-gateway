package com.ordersystem.order_service.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor 
@Builder

public class OrderItem {

    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private UUID productId;

    private Integer quantity;

    private BigDecimal unitPrice;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}
