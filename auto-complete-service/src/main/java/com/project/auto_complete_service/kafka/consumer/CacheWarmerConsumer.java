package com.project.auto_complete_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.auto_complete_service.model.SearchEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmerConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX    = "ac:";
    private static final String TRENDING_PREFIX = "trending:";
    private static final String USER_PREFIX     = "user:";     // ← new
    private static final double REALTIME_BOOST  = 1.0;
    private static final int    MAX_ZSET_SIZE   = 20;

    @KafkaListener(
            topics = "search-queries",
            groupId = "cache-warmer-group",
            concurrency = "3",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String payload, Acknowledgment ack) {
        try {
            SearchEvent event = objectMapper.readValue(payload, SearchEvent.class);

            String query     = event.query();
            String sessionId = event.sessionId();

            if (query == null || query.isBlank()) {
                ack.acknowledge();
                return;
            }

            String normalized = query.toLowerCase().trim()
                    .replaceAll("[^a-z0-9 ]", "");

            if (!normalized.isBlank()) {
                updateSuggestionCache(normalized);       // existing
                updateTrendingWindow(normalized);        // existing
                updatePersonalHistory(normalized, sessionId); // ← new
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("CacheWarmerConsumer error: {}", e.getMessage());
        }
    }

    // ── Existing: ac: prefix keys ────────────────────────────────
    private void updateSuggestionCache(String query) {
        for (int i = 1; i <= query.length(); i++) {
            String cacheKey = CACHE_PREFIX + query.substring(0, i);
            redisTemplate.opsForZSet()
                    .incrementScore(cacheKey, query, REALTIME_BOOST);
            Long size = redisTemplate.opsForZSet().size(cacheKey);
            if (size != null && size > MAX_ZSET_SIZE) {
                redisTemplate.opsForZSet().removeRange(cacheKey, 0, size - MAX_ZSET_SIZE - 1);
            }
        }
    }

    // ── Existing: trending: hour bucket ─────────────────────────
    private void updateTrendingWindow(String query) {
        String key = TRENDING_PREFIX + getCurrentHourBucket();
        redisTemplate.opsForZSet().incrementScore(key, query, 1.0);
        redisTemplate.expire(key, Duration.ofHours(2));
        Long size = redisTemplate.opsForZSet().size(key);
        if (size != null && size > 100) {
            redisTemplate.opsForZSet().removeRange(key, 0, size - 101);
        }
    }

    // ── New: user:{sessionId}:history ────────────────────────────
    private void updatePersonalHistory(String query, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;

        String userKey = USER_PREFIX + sessionId + ":history";

        // Increment score for this query in user's personal history
        redisTemplate.opsForZSet()
                .incrementScore(userKey, query, 1.0);

        // Keep TTL fresh — 30 days from last search
        redisTemplate.expire(userKey, Duration.ofDays(30));

        // Keep history bounded to top 200 terms per user
        Long size = redisTemplate.opsForZSet().size(userKey);
        if (size != null && size > 200) {
            redisTemplate.opsForZSet().removeRange(userKey, 0, size - 201);
        }

        log.debug("Personal history updated: user={} query={}", sessionId, query);
    }

    private String getCurrentHourBucket() {
        return LocalDateTime.now()
                .truncatedTo(ChronoUnit.HOURS)
                .toString();
    }
}