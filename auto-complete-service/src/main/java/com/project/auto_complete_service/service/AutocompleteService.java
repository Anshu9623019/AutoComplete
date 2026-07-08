package com.project.auto_complete_service.service;

import com.project.auto_complete_service.tries.AutocompleteTrie;
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
public class AutocompleteService {

    private final AutocompleteTrie trie;
    private final RedisTemplate<String, String> redisTemplate;
    private final PersonalizationService personalizationService;
    private final LlmService llmService;
    private final LlmCacheService llmCacheService;
    private final BKTreeService bkTreeService;
    private final VectorSearchService vectorSearchService; // ← inject

    private static final String   CACHE_PREFIX = "ac:";
    private static final Duration CACHE_TTL    = Duration.ofMinutes(5);

    @Value("${autocomplete.llm.min-trie-results:2}")
    private int minTrieResults;

    public List<String> getPersonalizedSuggestions(
            String prefix, String sessionId, int limit) {

        // ── Tier 1: Trie + Redis ──────────────────────────────────
        List<String> candidates = fetchFromCacheOrTrie(prefix, limit * 2);
        if (candidates.size() >= minTrieResults) {
            log.debug("Tier 1 (Trie) served '{}'", prefix);
            return personalizationService.blend(candidates, sessionId, limit);
        }

        // ── Tier 2: BK-Tree fuzzy ─────────────────────────────────
        List<String> fuzzy = bkTreeService.search(prefix, limit);
        if (!fuzzy.isEmpty()) {
            log.debug("Tier 2 (BK-Tree) served '{}'", prefix);
            return personalizationService.blend(
                    mergeResults(candidates, fuzzy, limit), sessionId, limit);
        }

        // ── Tier 3: LLM Redis cache ───────────────────────────────
        List<String> llmCached = llmCacheService.getCached(prefix);
        if (!llmCached.isEmpty()) {
            log.debug("Tier 3 (LLM cache) served '{}'", prefix);
            return personalizationService.blend(
                    mergeResults(candidates, llmCached, limit), sessionId, limit);
        }

        // ── Tier 4: pgvector semantic search ─────────────────────
        List<String> semantic = vectorSearchService.search(prefix, limit);
        if (!semantic.isEmpty()) {
            log.info("Tier 4 (pgvector) served '{}'", prefix);
            // Cache semantic results in LLM cache — same TTL, avoids re-embedding
            llmCacheService.cache(prefix, semantic);
            return personalizationService.blend(
                    mergeResults(candidates, semantic, limit), sessionId, limit);
        }

        // ── Tier 5: Groq API (last resort) ───────────────────────
        log.info("Tier 5 (Groq) serving '{}'", prefix);
        List<String> llmFresh = llmService.expandQuery(prefix, limit);
        if (!llmFresh.isEmpty()) {
            llmCacheService.cache(prefix, llmFresh);
        }

        return personalizationService.blend(
                mergeResults(candidates, llmFresh, limit), sessionId, limit);
    }

    // ── Update getSuggestionsWithSource to include SEMANTIC ───────
    public List<SuggestionSource> getSuggestionsWithSource(
            String prefix, String sessionId, int limit) {

        List<String> trieResults   = fetchFromCacheOrTrie(prefix, limit * 2);
        List<String> fuzzyResults  = trieResults.size() < minTrieResults
                ? bkTreeService.search(prefix, limit) : List.of();

        boolean needsAi = trieResults.size() < minTrieResults
                          && fuzzyResults.isEmpty();

        List<String> semanticResults = needsAi
                ? vectorSearchService.search(prefix, limit) : List.of();

        List<String> llmResults = needsAi && semanticResults.isEmpty()
                ? getLlmResults(prefix, limit) : List.of();

        Set<String> trieSet     = new HashSet<>(trieResults);
        Set<String> fuzzySet    = new HashSet<>(fuzzyResults);
        Set<String> semanticSet = new HashSet<>(semanticResults);

        List<String> all = mergeResults(
                mergeResults(
                        mergeResults(trieResults, fuzzyResults, limit * 3),
                        semanticResults, limit * 3),
                llmResults, limit * 3);

        List<String> blended = personalizationService
                .blend(all, sessionId, limit);

        return blended.stream()
                .map(w -> new SuggestionSource(
                        w,
                        trieSet.contains(w)     ? "TRIE"     :
                        fuzzySet.contains(w)    ? "FUZZY"    :
                        semanticSet.contains(w) ? "SEMANTIC" : "LLM"
                ))
                .toList();
    }

    private List<String> getLlmResults(String prefix, int limit) {
        List<String> cached = llmCacheService.getCached(prefix);
        if (!cached.isEmpty()) return cached;
        List<String> fresh = llmService.expandQuery(prefix, limit);
        if (!fresh.isEmpty()) llmCacheService.cache(prefix, fresh);
        return fresh;
    }


    // seach word in cache, if not found check it ties put result in to cache also keep TTL of 5 min on this extrated result from tries
    private List<String> fetchFromCacheOrTrie(String prefix, int limit) {
        String cacheKey = CACHE_PREFIX + prefix;
        Set<String> cached = redisTemplate.opsForZSet()         // todo : how is this working?
                .reverseRange(cacheKey, 0, limit - 1);

        if (cached != null && !cached.isEmpty()) return new ArrayList<>(cached);

        List<String> results = trie.search(prefix, limit);
        if (!results.isEmpty()) {
            for (int i = 0; i < results.size(); i++) {
                redisTemplate.opsForZSet()
                        .add(cacheKey, results.get(i), results.size() - i);
            }
            redisTemplate.expire(cacheKey, CACHE_TTL);
        }
        return results;
    }

    private List<String> mergeResults(
            List<String> primary, List<String> secondary, int limit) {
        List<String> merged = new ArrayList<>(primary);
        secondary.stream()
                .filter(w -> !merged.contains(w))
                .forEach(merged::add);
        return merged.stream().limit(limit).toList();
    }

    public record SuggestionSource(String word, String source) {}
}




