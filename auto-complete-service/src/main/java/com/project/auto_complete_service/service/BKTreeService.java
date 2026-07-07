package com.project.auto_complete_service.service;

import com.project.auto_complete_service.bktree.BKTree;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BKTreeService {

    private final BKTree bkTree;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String   FUZZY_PREFIX    = "fuzzy:";
    private static final Duration FUZZY_TTL       = Duration.ofHours(1);
    private static final int      TOLERANCE_CLOSE = 1;
    private static final int      TOLERANCE_FAR   = 2;

    public List<String> search(String query, int limit) {
        if (query == null || query.isBlank()) return List.of();

        // Step 1 — check fuzzy Redis cache
        List<String> cached = getFuzzyCache(query);
        if (!cached.isEmpty()) {
            log.debug("Fuzzy cache HIT for '{}'", query);
            return cached.stream().limit(limit).toList();
        }

        // Step 2 — tolerance 1 (close typos)
        // ✅ wrap in new ArrayList — bkTree.search() returns unmodifiable list
        List<BKTree.SearchResult> results =
                new ArrayList<>(bkTree.search(query, TOLERANCE_CLOSE, limit));

        // Step 3 — tolerance 2 (farther typos) if not enough results
        if (results.size() < limit) {
            List<BKTree.SearchResult> farResults =
                    bkTree.search(query, TOLERANCE_FAR, limit * 2);

            Set<String> found = results.stream()
                    .map(BKTree.SearchResult::word)
                    .collect(java.util.stream.Collectors.toSet());

            farResults.stream()
                    .filter(r -> !found.contains(r.word()))
                    .forEach(results::add);
        }

        // ✅ Now safe to sort — results is a mutable ArrayList
        results.sort((a, b) -> Integer.compare(a.distance(), b.distance()));

        List<String> words = results.stream()
                .limit(limit)
                .map(BKTree.SearchResult::word)
                .toList();

        if (!words.isEmpty()) {
            cacheFuzzy(query, words);
        }

        log.info("Fuzzy search '{}' → {} results", query, words.size());
        return words;
    }

    private List<String> getFuzzyCache(String query) {
        String key = FUZZY_PREFIX + query;
        Set<String> cached = redisTemplate.opsForZSet()
                .reverseRange(key, 0, -1);
        return cached != null && !cached.isEmpty()
                ? new ArrayList<>(cached)
                : List.of();
    }

    private void cacheFuzzy(String query, List<String> words) {
        String key = FUZZY_PREFIX + query;
        for (int i = 0; i < words.size(); i++) {
            redisTemplate.opsForZSet()
                    .add(key, words.get(i), words.size() - i);
        }
        redisTemplate.expire(key, FUZZY_TTL);
    }

    public void insert(String word) {
        bkTree.insert(word);
    }

    public boolean contains(String word) {
        List<BKTree.SearchResult> results = bkTree.search(word, 0, 1);
        return !results.isEmpty()
               && results.get(0).word().equals(word)
               && results.get(0).distance() == 0;
    }
}