package com.project.auto_complete_service.scheduler;

import com.project.auto_complete_service.model.QueryFrequency;
import com.project.auto_complete_service.repository.QueryFrequencyRepository;
import com.project.auto_complete_service.service.EmbeddingService;
import com.project.auto_complete_service.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingBackfillJob {

    private final QueryFrequencyRepository repo;
    private final EmbeddingService embeddingService;
    private final VectorSearchService vectorSearchService;

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void backfill() {
        List<QueryFrequency> words = repo.findTopWordsWithoutEmbeddings(100);

        if (words.isEmpty()) {
            log.debug("All top words have embeddings — nothing to backfill");
            return;
        }

        log.info("Backfilling embeddings for {} words", words.size());

        List<String> texts = words.stream()
                .map(QueryFrequency::getWord)
                .toList();

        List<float[]> embeddings = embeddingService.embedBatch(texts);

        if (embeddings.isEmpty() || embeddings.size() != words.size()) {
            log.warn("Embedding count mismatch — skipping batch");
            return;
        }

        int stored = 0;
        for (int i = 0; i < words.size(); i++) {
            float[] emb = embeddings.get(i);
            if (emb != null && emb.length > 0) {
                // ✅ Use native SQL with explicit CAST — bypasses Hibernate type issue
                String vectorStr = vectorSearchService.toVectorString(emb);
                repo.updateEmbedding(words.get(i).getId(), vectorStr);
                stored++;
            }
        }

        log.info("Stored {} embeddings in this backfill run", stored);
    }
}