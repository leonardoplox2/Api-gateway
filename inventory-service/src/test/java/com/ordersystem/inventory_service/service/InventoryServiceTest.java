package com.ordersystem.inventory_service.service;

import com.ordersystem.inventory_service.entity.Product;
import com.ordersystem.inventory_service.exception.InsufficientStockException;
import com.ordersystem.inventory_service.exception.ProductNotFoundException;
import com.ordersystem.inventory_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void reservarStockConDisponibilidadSuficienteDescuentaCantidad() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .name("Laptop")
                .price(BigDecimal.valueOf(1500))
                .availableQuantity(10)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        Product result = inventoryService.reserveStock(productId, 3);

        assertEquals(7, result.getAvailableQuantity());
        verify(productRepository).save(product);
    }

    @Test
    void reservarMasStockDelDisponibleLanzaExcepcion() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .name("Laptop")
                .price(BigDecimal.valueOf(1500))
                .availableQuantity(5)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class, () -> inventoryService.reserveStock(productId, 100));
    }

    @Test
    void reservarStockDeProductoInexistenteLanzaExcepcion() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> inventoryService.reserveStock(productId, 1));
    }
}