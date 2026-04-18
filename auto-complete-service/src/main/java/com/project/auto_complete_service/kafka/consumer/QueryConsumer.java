package com.project.auto_complete_service.kafka.consumer;


import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class QueryConsumer {

    private final Map<String, Integer> buffer = new ConcurrentHashMap<>();

    @KafkaListener(topics = "search-queries", groupId = "group1")
    public void consume(String query) {
        buffer.merge(query.toLowerCase(), 1, Integer::sum);
    }

    @Scheduled(fixedDelay = 60000)
    public void flush() {
        // TODO: save to DB in batch
        System.out.println("Flushing: " + buffer);
        buffer.clear();
    }
}
