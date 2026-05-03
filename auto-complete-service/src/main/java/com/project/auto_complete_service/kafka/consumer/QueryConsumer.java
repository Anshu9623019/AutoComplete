package com.project.auto_complete_service.kafka.consumer;

import com.project.auto_complete_service.repository.QueryFrequencyJdbcRepository;
import com.project.auto_complete_service.repository.QueryFrequencyRepository;
import com.project.auto_complete_service.service.QueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueryConsumer {

    private final ConcurrentHashMap<String, Integer> buffer =
            new ConcurrentHashMap<>();

    private final QueryService queryService;

    @KafkaListener(
            topics = "search-queries",
            groupId = "group5",
            concurrency = "3"
    )
    public void consume(String query, Acknowledgment ack) {

        try {

            if (query == null || query.isBlank()) {
                ack.acknowledge();
                return;
            }

            String normalized = query.toLowerCase()
                    .trim()
                    .replaceAll("[^a-z0-9 ]", "");

            if (!normalized.isBlank()) {
                buffer.merge(normalized, 1, Integer::sum);
            }

            ack.acknowledge();

        } catch (Exception e) {

            log.error("Error consuming query='{}': {}",
                    query,
                    e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void flush() {

        if (buffer.isEmpty()) {
            return;
        }

        Map<String, Integer> snapshot = new HashMap<>(buffer);

        log.info("Flushing {} unique terms to DB", snapshot.size());

        try {

            // bulk insert/update
            queryService.bulkFlush(snapshot);

            // clear only AFTER successful DB write
            snapshot.keySet().forEach(buffer::remove);

            log.info("Successfully flushed {} records",
                    snapshot.size());

        } catch (Exception e) {

            log.error("DB flush failed: {}", e.getMessage(), e);

            // optional:
            // publish failed snapshot to DLQ topic
        }
    }
}