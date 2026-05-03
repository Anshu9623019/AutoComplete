package com.project.auto_complete_service.kafka.consumer;


import com.project.auto_complete_service.service.QueryService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryConsumer {

    private final ConcurrentHashMap<String, Integer> buffer =
            new ConcurrentHashMap<>();

    private final QueryService queryService;

    @KafkaListener(
            topics = "search-queries",
            groupId = "frequency-aggregator-group",
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

            queryService.bulkFlush(snapshot);

            snapshot.forEach((key, value) ->
                    buffer.computeIfPresent(key, (k, current) -> {

                        int updated = current - value;

                        return updated <= 0 ? null : updated;
                    }));

            log.info("Successfully flushed {} records",
                    snapshot.size());

        } catch (Exception e) {

            log.error("DB flush failed: {}", e.getMessage(), e);
        }
    }
}