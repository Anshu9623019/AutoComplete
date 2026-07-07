package com.project.auto_complete_service.kafka.producer;

import com.project.auto_complete_service.model.SearchEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "search-queries";

    public void publish(String query, String sessionId) {
        if (query == null || query.isBlank()) return;

        try {
            SearchEvent event = new SearchEvent(query, sessionId);
            String payload = objectMapper.writeValueAsString(event);
            String partitionKey = String.valueOf(query.charAt(0));

            kafkaTemplate.send(TOPIC, partitionKey, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish query='{}': {}", query, ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error serializing search event: {}", e.getMessage());
        }
    }
}