package com.ordersystem.inventory_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ReleaseStockRequest(
        @NotNull UUID productId,
        @NotNull @Positive Integer quantity
) {
}
