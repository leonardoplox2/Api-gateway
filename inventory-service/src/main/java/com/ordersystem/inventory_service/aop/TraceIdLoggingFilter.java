package com.ordersystem.inventory_service.aop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class TraceIdLoggingFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        log.info("Request {} {} - traceId={}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                traceId);
        return chain.filter(exchange);
    }
}