package com.project.auto_complete_service.repository;

import com.project.auto_complete_service.model.QueryFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Stream;

@Repository
public interface QueryFrequencyRepository
                extends JpaRepository<QueryFrequency, Long> {

        // Existing methods
        @Query("SELECT q FROM QueryFrequency q ORDER BY q.frequency DESC")
        Stream<QueryFrequency> streamAllByOrderByFrequencyDesc();

        @Modifying
        @Query(value = """
                        INSERT INTO query_frequency (word, frequency)
                        VALUES (:word, :count)
                        ON CONFLICT (word)
                        DO UPDATE SET frequency = query_frequency.frequency + :count
                        """, nativeQuery = true)
        void upsert(@Param("word") String word, @Param("count") int count);

        @Query("SELECT q FROM QueryFrequency q ORDER BY q.frequency DESC")
        List<QueryFrequency> findTopByFrequency(
                        org.springframework.data.domain.Pageable pageable);

        default List<QueryFrequency> findTopByFrequency(int limit) {
                return findTopByFrequency(
                                org.springframework.data.domain.PageRequest.of(0, limit));
        }

        // ── NEW: vector similarity search ─────────────────────────────
        // Uses pgvector <=> operator (cosine distance)
        // cosine distance = 1 - cosine similarity
        // So ORDER BY distance ASC = ORDER BY similarity DESC
        @Query(value = """
                        SELECT word,
                               frequency,
                               1 - (embedding <=> CAST(:embedding AS vector)) AS similarity
                        FROM query_frequency
                        WHERE embedding IS NOT NULL
                          AND 1 - (embedding <=> CAST(:embedding AS vector)) >= :minSimilarity
                        ORDER BY embedding <=> CAST(:embedding AS vector)
                        LIMIT :limit
                        """, nativeQuery = true)
        List<Object[]> findSimilarByEmbedding(
                        @Param("embedding") String embedding, // pgvector string format
                        @Param("minSimilarity") double minSimilarity,
                        @Param("limit") int limit);

        // ── Find words without embeddings (for batch generation) ──────
        @Query("SELECT q FROM QueryFrequency q WHERE q.embedding IS NULL " +
                        "ORDER BY q.frequency DESC")
        List<QueryFrequency> findWordsWithoutEmbeddings(
                        org.springframework.data.domain.Pageable pageable);

        default List<QueryFrequency> findTopWordsWithoutEmbeddings(int limit) {
                return findWordsWithoutEmbeddings(
                                org.springframework.data.domain.PageRequest.of(0, limit));
        }
        // Add to QueryFrequencyRepository

        @Modifying
        @Query(value = """
                        UPDATE query_frequency
                        SET embedding = CAST(:embedding AS vector)
                        WHERE id = :id
                        """, nativeQuery = true)
        void updateEmbedding(@Param("id") Long id,
                        @Param("embedding") String embedding);
}