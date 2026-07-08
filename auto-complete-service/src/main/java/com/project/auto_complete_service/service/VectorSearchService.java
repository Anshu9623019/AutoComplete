package com.project.auto_complete_service.service;

import com.project.auto_complete_service.model.QueryFrequency;
import com.project.auto_complete_service.repository.QueryFrequencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {

    private final QueryFrequencyRepository repo;
    private final EmbeddingService embeddingService;

    @Value("${autocomplete.semantic.min-similarity:0.75}")
    private double minSimilarity;

    @Value("${autocomplete.semantic.max-results:5}")
    private int maxResults;

    @Value("${autocomplete.semantic.enabled:true}")
    private boolean enabled;

    // ── Main: search by semantic meaning ──────────────────────────
    // In VectorSearchService.search()
public List<String> search(String query, int limit) {
    if (!enabled || query == null || query.isBlank()) return List.of();

    try {
        float[] queryEmbedding = embeddingService.embed(query);
        if (queryEmbedding.length == 0) return List.of();

        String vectorStr = toVectorString(queryEmbedding);

        // ✅ Lower threshold for short queries — prefixes are semantically vague
        double effectiveThreshold = query.length() <= 3
                ? 0.4   // very lenient for short prefixes
                : query.length() <= 5
                ? 0.55  // medium for medium prefixes
                : minSimilarity; // full threshold for longer natural language queries

        List<Object[]> rows = repo.findSimilarByEmbedding(
                vectorStr, effectiveThreshold, limit);

        List<String> results = rows.stream()
                .map(row -> (String) row[0])
                .collect(Collectors.toList());

        log.info("Vector search '{}' → {} results (threshold={})",
                query, results.size(), effectiveThreshold);

        return results;

    } catch (Exception e) {
        log.warn("Vector search failed for '{}': {}", query, e.getMessage());
        return List.of();
    }
}

    @Transactional
    public void generateAndStoreEmbedding(String word) {
        try {
            // ✅ Check with a targeted query — not findAll()
            boolean alreadyHasEmbedding = repo.hasEmbedding(word);
            if (alreadyHasEmbedding) {
                log.debug("Embedding already exists for '{}'", word);
                return;
            }

            float[] embedding = embeddingService.embed(word);
            if (embedding.length > 0) {
                String vectorStr = toVectorString(embedding);
                repo.updateEmbedding(
                        repo.findIdByWord(word),  // just get the id
                        vectorStr
                );
                log.debug("Stored embedding for '{}'", word);
            }
        } catch (Exception e) {
            log.warn("Failed to store embedding for '{}': {}", word, e.getMessage());
        }
    }

    // ── Convert float[] to pgvector string: "[0.1,0.2,...]" ──────
    public String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public boolean isEnabled() { return enabled; }
}