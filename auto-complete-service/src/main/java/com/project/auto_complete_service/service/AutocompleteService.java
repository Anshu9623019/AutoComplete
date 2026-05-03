package com.project.auto_complete_service.service;

import com.project.auto_complete_service.tries.AutocompleteTrie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutocompleteService {

    private final AutocompleteTrie trie;
    private final RedisTemplate<String, String> redisTemplate;  // ← inject Redis

    private static final String CACHE_PREFIX = "ac:";
    private static final Duration CACHE_TTL   = Duration.ofMinutes(5);

//    public List<String> getSuggestions(String prefix, int limit) {
//        String cacheKey = CACHE_PREFIX + prefix;  // e.g. "ac:java"
//
//        // ── STEP 1: check Redis first ──────────────────────────────
//        Set<String> cached = redisTemplate.opsForZSet()
//                .reverseRange(cacheKey, 0, limit - 1);  // sorted by score desc
//
//        if (cached != null && !cached.isEmpty()) {
//            log.debug("Redis HIT for prefix='{}'", prefix);
//            return new ArrayList<>(cached);
//        }
//
//        // ── STEP 2: Redis miss → go to Trie ───────────────────────
//        log.debug("Redis MISS for prefix='{}', querying Trie", prefix);
//        List<String> results = trie.search(prefix, limit);
//
//        // ── STEP 3: populate Redis for next time ───────────────────
//        if (!results.isEmpty()) {
//            // ZSet score = position index (0 = highest rank)
//            // We store rank as negative so ZREVRANGE gives correct order
//            for (int i = 0; i < results.size(); i++) {
//                redisTemplate.opsForZSet()
//                        .add(cacheKey, results.get(i), results.size() - i);
//            }
//            redisTemplate.expire(cacheKey, CACHE_TTL);
//            log.debug("Populated Redis cache for prefix='{}' with {} items", prefix, results.size());
//        }
//
//        return results;
//    }

    public List<String> getSuggestions(String prefix, int limit) {

        prefix = prefix.toLowerCase().trim();

        String cacheKey = CACHE_PREFIX + prefix;

        log.info("Checking Redis key={}", cacheKey);

        Set<String> cached = redisTemplate.opsForZSet()
                .reverseRange(cacheKey, 0, limit - 1);

        log.info("CACHE RESULT={}", cached);

        if (cached != null && !cached.isEmpty()) {
            log.info("REDIS HIT");
            return new ArrayList<>(cached);
        }

        log.info("REDIS MISS");

        List<String> results = trie.search(prefix, limit);

        log.info("TRIE RESULTS={}", results);

        if (!results.isEmpty()) {

            for (int i = 0; i < results.size(); i++) {
                redisTemplate.opsForZSet()
                        .add(cacheKey, results.get(i), results.size() - i);
            }

            redisTemplate.expire(cacheKey, CACHE_TTL);

            log.info("WROTE TO REDIS");

            Set<String> verify = redisTemplate.opsForZSet()
                    .reverseRange(cacheKey, 0, limit - 1);

            log.info("VERIFY READ={}", verify);

            Boolean exists = redisTemplate.hasKey(cacheKey);

            log.info("KEY EXISTS={}", exists);
        }

        return results;
    }
}