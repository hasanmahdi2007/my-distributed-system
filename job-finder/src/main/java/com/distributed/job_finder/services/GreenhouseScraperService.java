package com.distributed.job_finder.services;

import com.distributed.job_finder.dtos.JobDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.UUID;

@Slf4j
@Service
public class GreenhouseScraperService {

    private final WebClient webClient;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // The Redis stream key where we will dump incoming jobs
    private static final String JOB_INGESTION_STREAM = "job:ingestion:stream";

    @Autowired
    public GreenhouseScraperService(WebClient webClient, RedisTemplate<String, Object> redisTemplate) {
        this.webClient = webClient;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Reaches out to a specific company's Greenhouse job board.
     * @param companyId The UUID of the company from our database
     * @param boardToken The unique Greenhouse token for the company (e.g., "careem", "talabat")
     */
    public void fetchJobsFromGreenhouse(UUID companyId, String boardToken) {
        String greenhouseApiUrl = "https://boards-api.greenhouse.io/v1/boards/" + boardToken + "/jobs";

        log.info("Fetching jobs from Greenhouse for board: {}", boardToken);

        webClient.get()
                .uri(greenhouseApiUrl)
                .retrieve()
                // For now, we will grab it as a raw String to see the structure
                .bodyToMono(String.class)
                .doOnSuccess(jsonResponse -> {
                    log.info("Successfully fetched data for {}", boardToken);
                    // Next up: We will parse this JSON string into our JobDto
                    // and push it to the redisTemplate!
                })
                .doOnError(error -> log.error("Failed to fetch from {}: {}", boardToken, error.getMessage()))
                .subscribe(); // Non-blocking execution
    }
}