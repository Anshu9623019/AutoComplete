package com.project.auto_complete_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrendingService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String TRENDING_PREFIX = "trending:";

    // ── Get top trending terms (current + previous hour) ──────────
    public List<TrendingTerm> getTrending(int limit) {
        String currentBucket  = getBucket(0);   // current hour
        String previousBucket = getBucket(-1);  // previous hour

        // Merge scores from both buckets into one map
        Map<String, Double> merged = new HashMap<>();
        addBucketScores(merged, currentBucket,  1.0);   // current hour full weight
        addBucketScores(merged, previousBucket, 0.5);   // previous hour half weight

        // Sort by merged score descending, return top N
        return merged.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new TrendingTerm(e.getKey(), e.getValue()))
                .toList();
    }

    // ── Get trending for a specific past hour (for analytics) ─────
    public List<TrendingTerm> getTrendingByHour(int hoursAgo, int limit) {
        String bucket = getBucket(-hoursAgo);
        Map<String, Double> scores = new HashMap<>();
        addBucketScores(scores, bucket, 1.0);

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new TrendingTerm(e.getKey(), e.getValue()))
                .toList();
    }

    // ── Check if a specific term is trending ──────────────────────
    public boolean isTrending(String term, double threshold) {
        String currentBucket = getBucket(0);
        Double score = redisTemplate.opsForZSet()
                .score(TRENDING_PREFIX + currentBucket, term);
        return score != null && score >= threshold;
    }

    // ── Internal helpers ──────────────────────────────────────────
    private void addBucketScores(Map<String, Double> map,
                                 String bucket, double weight) {
        String key = TRENDING_PREFIX + bucket;
        Set<ZSetOperations.TypedTuple<String>> entries =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, 99);

        if (entries == null) return;

        entries.forEach(e -> {
            if (e.getValue() != null && e.getScore() != null) {
                map.merge(e.getValue(),
                        e.getScore() * weight,
                        Double::sum);
            }
        });
    }

    private String getBucket(int hoursOffset) {
        return LocalDateTime.now()
                .plusHours(hoursOffset)
                .truncatedTo(ChronoUnit.HOURS)
                .toString();
    }

    // ── Response record ───────────────────────────────────────────
    public record TrendingTerm(String word, double score) {}
}