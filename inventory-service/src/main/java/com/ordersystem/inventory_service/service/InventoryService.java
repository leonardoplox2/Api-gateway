package com.ordersystem.inventory_service.service;

import com.ordersystem.inventory_service.entity.Product;
import com.ordersystem.inventory_service.exception.InsufficientStockException;
import com.ordersystem.inventory_service.exception.ProductNotFoundException;
import com.ordersystem.inventory_service.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InventoryService {

    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product reserveStock(UUID productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Producto no encontrado: " + productId));

        if (product.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Stock insuficiente para el producto " + productId
                            + ". Disponible: " + product.getAvailableQuantity() + ", solicitado: " + quantity);
        }

        product.setAvailableQuantity(product.getAvailableQuantity() - quantity);
        return productRepository.save(product);
    }

    public Product releaseStock(UUID productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Producto no encontrado: " + productId));

        product.setAvailableQuantity(product.getAvailableQuantity() + quantity);
        return productRepository.save(product);
    }
}