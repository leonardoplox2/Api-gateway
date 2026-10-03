package com.ordersystem.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutesConfig {

    @Value("${order.service.base-url}")
    private String orderServiceBaseUrl;

    @Value("${inventory.service.base-url}")
    private String inventoryServiceBaseUrl;

    @Bean
    public RouteLocator customRouterLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("order-service", r -> r.path("/api/orders/**")
                        .uri(orderServiceBaseUrl))
                .route("inventory-service", r -> r.path("/api/inventory/**")
                        .uri(inventoryServiceBaseUrl))
                .build();
    }
}
