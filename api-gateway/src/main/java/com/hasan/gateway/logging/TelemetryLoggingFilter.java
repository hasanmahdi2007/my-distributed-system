package com.hasan.gateway.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TelemetryLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TelemetryLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        
        // =======================================================
        // PHASE 1: START CLOCK & COLLECT INBOUND DATA
        // =======================================================
        long startTime = System.currentTimeMillis();
        String correlationId = UUID.randomUUID().toString();

        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getPath().value();
        
        // Safely extract IP and Headers
        String clientIp = request.getRemoteAddress() != null ? 
                          request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        String userAgent = request.getHeaders().getFirst("User-Agent");
        long requestSizeBytes = request.getHeaders().getContentLength(); // Returns -1 if unknown

        // =======================================================
        // PHASE 2: MUTATE REQUEST & PACK THE BACKPACK
        // =======================================================
        // Inject the Correlation ID into the headers for downstream services
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Correlation-ID", correlationId)
                .build();

        // Package the mutated request into a new exchange
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        // =======================================================
        // PHASE 3 & 4: ROUTE AND WAIT FOR THE RETURN JOURNEY
        // =======================================================
        return chain.filter(mutatedExchange).then(Mono.fromRunnable(() -> {
            
            // Stop the clock! The backend has replied.
            long totalLatencyMs = System.currentTimeMillis() - startTime;
            
            ServerHttpResponse response = mutatedExchange.getResponse();
            
            // Safely extract Status Code and Response Size
            int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 500;
            long responseSizeBytes = response.getHeaders().getContentLength(); // Returns -1 if unknown

            // Reach into the backpack to get the API Key left by the downstream Auth Filter
            // (Assumes your future Auth filter saves it under the key "client_api_key")
            String apiKey = mutatedExchange.getAttributeOrDefault("client_api_key", "unauthenticated");

            // Build the final telemetry payload
            log.info("TRACE: {} | Method: {} | Path: {} | Status: {} | Latency: {}ms | IP: {} | User-Agent: {} | ReqBytes: {} | ResBytes: {} | API Key: {}", 
                     correlationId, method, path, statusCode, totalLatencyMs, clientIp, userAgent, requestSizeBytes, responseSizeBytes, apiKey);
        }));
    }

    @Override
    public int getOrder() {
        // Runs at the absolute perimeter of the gateway
        return -4;
    }
}