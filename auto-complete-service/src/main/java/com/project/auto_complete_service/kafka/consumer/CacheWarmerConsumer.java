package com.project.auto_complete_service.kafka.consumer;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmerConsumer {

    private final RedisTemplate<String, String> redisTemplate;  // ← inject Redis

    private static final String CACHE_PREFIX   = "ac:";
    private static final double REALTIME_BOOST = 1.0;
    private static final int    MAX_ZSET_SIZE  = 20;

//    @KafkaListener(
//            topics = "search-queries",
//            groupId = "cache-warmer-group",   // ← separate group from aggregator
//            concurrency = "3"
//    )
//    public void consume(String query, Acknowledgment ack) {
//        try {
//            if (query == null || query.isBlank()) {
//                ack.acknowledge();
//                return;
//            }
//
//            String normalized = query.toLowerCase().trim();
//
//            // Update Redis for every prefix of this query
//            // e.g. "java" → updates ac:j, ac:ja, ac:jav, ac:java
//            for (int i = 1; i <= normalized.length(); i++) {
//                String prefix   = normalized.substring(0, i);
//                String cacheKey = CACHE_PREFIX + prefix;
//
//                // Increment score of this word under this prefix key
//                redisTemplate.opsForZSet()
//                        .incrementScore(cacheKey, normalized, REALTIME_BOOST);
//
//                // Keep the ZSet bounded — remove lowest scoring beyond top 20
//                Long size = redisTemplate.opsForZSet().size(cacheKey);
//                if (size != null && size > MAX_ZSET_SIZE) {
//                    redisTemplate.opsForZSet().removeRange(cacheKey, 0, size - MAX_ZSET_SIZE - 1);
//                }
//            }
//
//            ack.acknowledge();
//        } catch (Exception e) {
//            log.error("CacheWarmerConsumer failed for query='{}': {}", query, e.getMessage());
//        }
//    }


    @KafkaListener(
            topics = "search-queries",
            groupId = "cache-warmer-group",
            concurrency = "3"
    )
    public void consume(String query, Acknowledgment ack) {

        log.info("MESSAGE RECEIVED = {}", query);

        try {

            if (query == null || query.isBlank()) {
                ack.acknowledge();
                return;
            }

            String normalized = query.toLowerCase().trim();

            for (int i = 1; i <= normalized.length(); i++) {

                String prefix = normalized.substring(0, i);
                String cacheKey = CACHE_PREFIX + prefix;

                Double score = redisTemplate.opsForZSet()
                        .incrementScore(cacheKey, normalized, REALTIME_BOOST);

                log.info("UPDATED REDIS KEY = {} VALUE = {} SCORE = {}",
                        cacheKey,
                        normalized,
                        score);

                Long size = redisTemplate.opsForZSet().size(cacheKey);

                if (size != null && size > MAX_ZSET_SIZE) {
                    redisTemplate.opsForZSet()
                            .removeRange(cacheKey, 0, size - MAX_ZSET_SIZE - 1);
                }
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("CacheWarmerConsumer failed", e);
        }
    }
}
