package com.project.auto_complete_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.auto_complete_service.model.SearchEvent;
import com.project.auto_complete_service.repository.QueryFrequencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class FrequencyAggregatorConsumer {

    private final QueryFrequencyRepository repo;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Integer> buffer
            = new ConcurrentHashMap<>();

    @KafkaListener(
            topics = "search-queries",
            groupId = "frequency-aggregator-group",
            concurrency = "3"
    )
    public void consume(String payload, Acknowledgment ack) {
        try {
            // Deserialize JSON event
            SearchEvent event = objectMapper.readValue(payload, SearchEvent.class);
            String query = event.query();

            if (query == null || query.isBlank()) {
                ack.acknowledge();
                return;
            }

            String normalized = query.toLowerCase().trim()
                    .replaceAll("[^a-z0-9 ]", "");

            if (!normalized.isBlank()) {
                buffer.merge(normalized, 1, Integer::sum);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("FrequencyAggregatorConsumer error: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void flush() {
        if (buffer.isEmpty()) return;

        Map<String, Integer> snapshot = new HashMap<>(buffer);
        buffer.clear();

        log.info("Flushing {} terms to DB", snapshot.size());
        snapshot.forEach((word, count) -> {
            try {
                repo.upsert(word, count);
            } catch (Exception e) {
                log.error("Failed to upsert word='{}': {}", word, e.getMessage());
            }
        });
    }
}