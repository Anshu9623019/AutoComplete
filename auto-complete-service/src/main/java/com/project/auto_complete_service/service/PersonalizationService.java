package com.project.auto_complete_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalizationService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String USER_PREFIX     = "user:";
    private static final double GLOBAL_WEIGHT   = 0.7;
    private static final double PERSONAL_WEIGHT = 0.3;

    // ── Blend global scores with personal history ─────────────────
    public List<String> blend(
            List<String> globalResults,   // from Trie/Redis — already ranked
            String sessionId,
            int limit) {

        if (sessionId == null || sessionId.isBlank()) {
            // No session → return global results as-is
            return globalResults.stream().limit(limit).toList();
        }

        String userKey = USER_PREFIX + sessionId + ":history";

        // Get personal scores for each candidate word
        Map<String, Double> blendedScores = new LinkedHashMap<>();

        for (int i = 0; i < globalResults.size(); i++) {
            String word = globalResults.get(i);

            // Global score: position-based (first result = highest score)
            // We use position because Trie already ranked them by frequency
            double globalScore = (globalResults.size() - i) * 10.0;

            // Personal score: how many times this user searched this word
            Double personalScore = redisTemplate.opsForZSet()
                    .score(userKey, word);

            double personal = (personalScore != null) ? personalScore : 0.0;

            // Blend: 70% global + 30% personal
            double finalScore = (globalScore * GLOBAL_WEIGHT)
                    + (personal    * PERSONAL_WEIGHT);

            blendedScores.put(word, finalScore);

            log.debug("word={} global={} personal={} final={}",
                    word, globalScore, personal, finalScore);
        }

        // Re-sort by blended score and return top N
        return blendedScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    // ── Get user's top searched terms (for analytics/debug) ───────
    public List<String> getUserHistory(String sessionId, int limit) {
        if (sessionId == null || sessionId.isBlank()) return List.of();

        String userKey = USER_PREFIX + sessionId + ":history";
        Set<String> history = redisTemplate.opsForZSet()
                .reverseRange(userKey, 0, limit - 1);

        return history != null ? new ArrayList<>(history) : List.of();
    }

    // ── Clear user history (GDPR / privacy) ──────────────────────
    public void clearUserHistory(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        redisTemplate.delete(USER_PREFIX + sessionId + ":history");
        log.info("Cleared history for session={}", sessionId);
    }
}
