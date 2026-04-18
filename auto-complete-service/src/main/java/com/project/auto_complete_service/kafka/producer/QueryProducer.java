package com.project.auto_complete_service.kafka.producer;


import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueryProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(String query) {
        kafkaTemplate.send("search-queries", query);
    }
}
