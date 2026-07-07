package com.project.auto_complete_service.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmCacheService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String LLM_PREFIX = "llm:";

    @Value("${autocomplete.llm.cache-ttl-minutes:60}")
    private long cacheTtlMinutes;

    // ── Get cached LLM results ────────────────────────────────────
    public List<String> getCached(String query) {
        String key = LLM_PREFIX + query;
        Set<String> cached = redisTemplate.opsForZSet()
                .reverseRange(key, 0, -1);

        if (cached != null && !cached.isEmpty()) {
            log.debug("LLM cache HIT for '{}'", query);
            return new ArrayList<>(cached);
        }

        log.debug("LLM cache MISS for '{}'", query);
        return List.of();
    }

    // ── Cache LLM results ─────────────────────────────────────────
    public void cache(String query, List<String> suggestions) {
        if (suggestions.isEmpty()) return;

        String key = LLM_PREFIX + query;

        // Store with score = position (first = highest rank)
        for (int i = 0; i < suggestions.size(); i++) {
            redisTemplate.opsForZSet()
                    .add(key, suggestions.get(i), suggestions.size() - i);
        }

        // Expire after 1 hour — LLM results can go stale
        redisTemplate.expire(key, Duration.ofMinutes(cacheTtlMinutes));
        log.debug("Cached {} LLM suggestions for '{}'", suggestions.size(), query);
    }

    // ── Check if already cached ───────────────────────────────────
    public boolean isCached(String query) {
        String key = LLM_PREFIX + query;
        Long size = redisTemplate.opsForZSet().size(key);
        return size != null && size > 0;
    }
}
