package com.project.auto_complete_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheWarmerService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String CACHE_PREFIX = "ac:";
    private static final double REALTIME_BOOST = 1.0;
    private static final int MAX_ZSET_SIZE = 20;

    public void process(String query) {

        if (query == null || query.isBlank()) {
            return;
        }

        String normalized = query.toLowerCase().trim();

        for (int i = 1; i <= normalized.length(); i++) {

            String prefix = normalized.substring(0, i);
            String cacheKey = CACHE_PREFIX + prefix;

            redisTemplate.opsForZSet()
                    .incrementScore(cacheKey, normalized, REALTIME_BOOST);

            Long size = redisTemplate.opsForZSet().size(cacheKey);

            if (size != null && size > MAX_ZSET_SIZE) {
                redisTemplate.opsForZSet()
                        .removeRange(cacheKey, 0, size - MAX_ZSET_SIZE - 1);
            }

            log.info("UPDATED REDIS KEY = {}", cacheKey);
        }
    }
}