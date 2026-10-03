package com.ordersystem.inventory_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;



@Entity 
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String description;
    
    private BigDecimal price;

    private Integer availableQuantity;

    @Version
    private Integer version;

}
