package com.project.auto_complete_service.controller;

import com.project.auto_complete_service.kafka.producer.QueryProducer;
import com.project.auto_complete_service.service.AutocompleteService;
import com.project.auto_complete_service.service.CacheWarmerService;
import com.project.auto_complete_service.service.FrequencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AutocompleteController {

    private final AutocompleteService service;
    private final QueryProducer       producer;
    private final RedisTemplate<String, String> redisTemplate;  // ← inject Redis
    private final CacheWarmerService cacheWarmerService;
    private final FrequencyService frequencyService;
    private static final int    RATE_LIMIT        = 30;    // max 30 requests
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    @GetMapping("/suggest")
    public ResponseEntity<List<String>> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit,
            @RequestHeader(value = "X-API-Key", defaultValue = "anonymous") String apiKey) {

        // ── Validate input ─────────────────────────────────────────
        if (q == null || q.isBlank() || q.length() > 50) {
            return ResponseEntity.badRequest().build();
        }
        if (limit < 1 || limit > 10) {
            return ResponseEntity.badRequest().build();
        }

        // ── Rate limit check (Redis) ───────────────────────────────
        if (isRateLimited(apiKey)) {
            log.warn("Rate limit exceeded for apiKey='{}'", apiKey);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        // ── Fetch suggestions (Redis → Trie) ───────────────────────
        String normalized = q.toLowerCase().trim();
        List<String> suggestions = service.getSuggestions(normalized, limit);

//        // ── Publish to Kafka (async, non-blocking) ─────────────────
//        producer.publish(normalized);

        cacheWarmerService.process(normalized);
        frequencyService.save(normalized);


        return ResponseEntity.ok(suggestions);
    }

    private boolean isRateLimited(String apiKey) {
        String rateLimitKey = "rate:" + apiKey;

        // Increment counter; if first request, set TTL
        Long count = redisTemplate.opsForValue().increment(rateLimitKey);
        if (count != null && count == 1) {
            // First request in this window — set expiry
            redisTemplate.expire(rateLimitKey, RATE_LIMIT_WINDOW);
        }
        return count != null && count > RATE_LIMIT;
    }
}