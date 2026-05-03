package com.project.auto_complete_service.repository;

import com.project.auto_complete_service.model.QueryFrequency;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Stream;


@Repository
public interface QueryFrequencyRepository extends JpaRepository<QueryFrequency, Long> {
    // ✅ Stream instead of List — rows are fetched in batches, not all at once
    @Query("""
            SELECT q
            FROM QueryFrequency q
            ORDER BY q.frequency DESC
            """)
    Stream<QueryFrequency> streamAllByOrderByFrequencyDesc();
    Optional<QueryFrequency> findByWord(String word);
}
