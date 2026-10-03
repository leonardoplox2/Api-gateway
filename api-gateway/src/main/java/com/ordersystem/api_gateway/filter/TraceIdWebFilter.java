package com.ordersystem.api_gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;

import reactor.core.publisher.Mono;

import java.util.UUID;

@Component 
@Order(Ordered.HIGHEST_PRECEDENCE) 
public class TraceIdWebFilter implements WebFilter{

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_ATTR = "traceId";

    @Override 
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId   = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(TRACE_ID_HEADER, traceId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();
        
        mutatedExchange.getAttributes().put(TRACE_ID_ATTR, traceId);
        mutatedExchange.getResponse().getHeaders().add(TRACE_ID_HEADER, traceId);

        return chain.filter(mutatedExchange);
    }

}
