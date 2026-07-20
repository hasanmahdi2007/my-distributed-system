package com.system.analytics_engine.services;

import com.system.analytics_engine.entities.ApiRequestLog;
import com.system.analytics_engine.repos.ApiRequestLogRepo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class TelemetryStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelemetryStreamConsumer.class);

    private final ReactiveRedisConnectionFactory redisConnectionFactory;
    private final ApiRequestLogRepo repository;

    public TelemetryStreamConsumer(ReactiveRedisConnectionFactory redisConnectionFactory, ApiRequestLogRepo repository) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.repository = repository;
    }

    @PostConstruct
    public void setup() {
        // 1. Automatically create the Consumer Group when the app starts
        redisConnectionFactory.getReactiveConnection()
            .streamCommands()
            .xGroupCreate(
                java.nio.ByteBuffer.wrap("telemetry:stream".getBytes()), 
                "analytics-group", 
                ReadOffset.from("0"), 
                true // MKSTREAM: Create the stream if the Gateway hasn't sent data yet
            )
            .onErrorResume(e -> {
                log.info("Consumer group 'analytics-group' already exists. Safe to proceed.");
                return reactor.core.publisher.Mono.empty();
            })
            .doOnSuccess(success -> startListening()) // 2. Once created, start collecting!
            .subscribe();
    }

    private void startListening() {
        StreamReceiver.StreamReceiverOptions<String, MapRecord<String, String, String>> options = 
                StreamReceiver.StreamReceiverOptions.builder()
                .pollTimeout(Duration.ofMillis(100))
                .build();

        StreamReceiver<String, MapRecord<String, String, String>> receiver = 
                StreamReceiver.create(redisConnectionFactory, options);

        log.info("Analytics Engine started listening to telemetry:stream for batches...");

        // 3. THE ACTUAL PRODUCTION BATCH COLLECTOR
        receiver.receiveAutoAck(
                Consumer.from("analytics-group", "engine-instance-1"), 
                StreamOffset.create("telemetry:stream", ReadOffset.lastConsumed())
        )
        .map(this::convertRecordToEntity)
        .bufferTimeout(50, Duration.ofSeconds(3)) // The collector: waits for 50 records OR 3 seconds
        .publishOn(Schedulers.boundedElastic())   // The thread hand-off for safety
        .doOnNext(this::saveBatchToDatabase)      // The PostgreSQL bulk save
        .subscribe();
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private ApiRequestLog convertRecordToEntity(MapRecord<String, String, String> record) {
        Map<String, String> map = record.getValue();
        ApiRequestLog logEntry = new ApiRequestLog();
        
        logEntry.setCorrelationId(map.get("correlationId"));
        logEntry.setMethod(map.get("method"));
        logEntry.setPath(map.get("path"));
        logEntry.setStatus(Integer.parseInt(map.getOrDefault("status", "0")));
        logEntry.setLatencyMs(Long.parseLong(map.getOrDefault("latencyMs", "0")));
        logEntry.setIp(map.get("ip"));
        logEntry.setUserAgent(map.get("userAgent"));
        logEntry.setReqBytes(Long.parseLong(map.getOrDefault("reqBytes", "0")));
        logEntry.setResBytes(Long.parseLong(map.getOrDefault("resBytes", "0")));
        logEntry.setApiKey(map.get("apiKey"));
        
        return logEntry;
    }

    private void saveBatchToDatabase(List<ApiRequestLog> batch) {
        if (!batch.isEmpty()) {
            repository.saveAll(batch);
            log.info("Successfully bulk inserted {} records into PostgreSQL.", batch.size());
        }
    }
}