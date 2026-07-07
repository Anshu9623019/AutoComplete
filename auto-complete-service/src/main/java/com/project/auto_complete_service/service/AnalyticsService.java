package com.project.auto_complete_service.service;

import com.project.auto_complete_service.repository.QueryFrequencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final QueryFrequencyRepository repo;
    private final RedisTemplate<String, String> redisTemplate;
    private final TrendingService trendingService;

    private static final String TRENDING_PREFIX = "trending:";

    // ── Full analytics snapshot ───────────────────────────────────
    public AnalyticsSnapshot getSnapshot() {
        return new AnalyticsSnapshot(
                getTopTerms(10),
                getSearchVolumeByHour(24),
                getTotalSearches(),
                getCacheHitRate(),
                getTrieSize(),
                getLlmCacheSize(),
                trendingService.getTrending(8)
        );
    }

    // ── Top N most searched terms from PostgreSQL ─────────────────
    public List<TermCount> getTopTerms(int limit) {
        return repo.findTopByFrequency(limit)
                .stream()
                .map(q -> new TermCount(q.getWord(), q.getFrequency()))
                .toList();
    }

    // ── Search volume per hour for last N hours ───────────────────
    public List<HourlyVolume> getSearchVolumeByHour(int hours) {
        List<HourlyVolume> volumes = new ArrayList<>();

        for (int i = hours - 1; i >= 0; i--) {
            LocalDateTime hour = LocalDateTime.now()
                    .truncatedTo(ChronoUnit.HOURS)
                    .minusHours(i);

            String bucket = hour.toString();
            String key    = TRENDING_PREFIX + bucket;

            // Count total searches in this bucket
            // by summing all scores in the ZSET
            Set<ZSetOperations.TypedTuple<String>> entries =
                    redisTemplate.opsForZSet()
                            .rangeWithScores(key, 0, -1);

            double total = 0;
            if (entries != null) {
                total = entries.stream()
                        .mapToDouble(e -> e.getScore() != null
                                ? e.getScore() : 0)
                        .sum();
            }

            volumes.add(new HourlyVolume(
                    hour.getHour() + ":00",
                    (long) total
            ));
        }

        return volumes;
    }

    // ── Total unique terms in DB ──────────────────────────────────
    public long getTotalSearches() {
        try {
            return repo.count();
        } catch (Exception e) {
            return 0;
        }
    }

    // ── Cache hit rate from Redis ─────────────────────────────────
    public double getCacheHitRate() {
        try {
            // Count ac: keys in Redis — proxy for cache usage
            Set<String> acKeys = redisTemplate.keys("ac:*");
            Set<String> llmKeys = redisTemplate.keys("llm:*");

            long acCount  = acKeys  != null ? acKeys.size()  : 0;
            long llmCount = llmKeys != null ? llmKeys.size() : 0;
            long total    = acCount + llmCount;

            if (total == 0) return 0.0;

            // ac: keys = trie cache hits, llm: = llm cache
            return Math.round((acCount / (double) total) * 100.0) / 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    // ── Trie cache key count ──────────────────────────────────────
    public long getTrieSize() {
        try {
            Set<String> keys = redisTemplate.keys("ac:*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ── LLM cache key count ───────────────────────────────────────
    public long getLlmCacheSize() {
        try {
            Set<String> keys = redisTemplate.keys("llm:*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ── Records ───────────────────────────────────────────────────
    public record AnalyticsSnapshot(
            List<TermCount> topTerms,
            List<HourlyVolume> searchVolumeByHour,
            long totalUniqueTerms,
            double cacheHitRate,
            long trieCacheSize,
            long llmCacheSize,
            List<TrendingService.TrendingTerm> trending
    ) {}

    public record TermCount(String word, int count) {}

    public record HourlyVolume(String hour, long count) {}
}