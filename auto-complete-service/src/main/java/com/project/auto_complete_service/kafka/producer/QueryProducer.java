package com.project.auto_complete_service.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
        private static final String TOPIC = "search-queries"; // ← no magic strings inline

    public void publish(String query) {
        if (query == null || query.isBlank()) return; // ← guard

        // Partition key = first char → all "java*" queries land on same partition
        // This keeps per-prefix ordering and makes aggregation efficient
        String partitionKey = String.valueOf(query.charAt(0));

        kafkaTemplate.send(TOPIC, partitionKey, query)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        // Log and move on — never block search response for analytics
                        log.warn("Failed to publish query='{}' to Kafka: {}", query, ex.getMessage());
                    } else {
                        log.debug("Published query='{}' to partition={}",
                                query, result.getRecordMetadata().partition());
                    }
                });
        // No .get() or .join() here — that would make it blocking and add latency
    }
}