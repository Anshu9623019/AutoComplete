package com.project.auto_complete_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmbeddingService {

    private final WebClient webClient;

    @Value("${embedding.cohere.api-key:}")
    private String apiKey;

    @Value("${embedding.cohere.model:embed-english-light-v3.0}")
    private String model;

    @Value("${embedding.cohere.enabled:true}")
    private boolean enabled;

    // public EmbeddingService(
    //         @Value("${embedding.cohere.base-url:https://api.cohere.ai/v1}") String baseUrl) {
    //     this.webClient = WebClient.builder()
    //             .baseUrl(baseUrl)
    //             .defaultHeader(HttpHeaders.CONTENT_TYPE,
    //                     MediaType.APPLICATION_JSON_VALUE)
    //             .build();
    // }

    public EmbeddingService(
        @Value("${embedding.cohere.base-url:https://api.cohere.ai/v1}")
        String baseUrl) {
    
    // Configure an increased buffer size strategy (10 MB)
    int maxBufferSize = 10 * 1024 * 1024; 
    
    this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .codecs(configurer -> configurer
                    .defaultCodecs()
                    .maxInMemorySize(maxBufferSize)) // ← Overrides the 256KB default limit
            .build();
}

    // ── Single embed ──────────────────────────────────────────────
    public float[] embed(String text) {
        if (!enabled || apiKey.isBlank()) {
            log.warn("Embedding disabled or API key missing");
            return new float[0];
        }

        try {
            // Cohere embed API format
            Map<String, Object> requestBody = Map.of(
                    "texts", List.of(text),
                    "model", model,
                    "input_type", "search_query" // ← required by Cohere
            );

            Map response = webClient.post()
                    .uri("/embed") // ← Cohere endpoint is /embed
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            return extractEmbedding(response, 0);

        } catch (Exception e) {
            log.warn("Embedding failed for '{}': {}", text, e.getMessage());
            return new float[0];
        }
    }

    // ── Batch embed ───────────────────────────────────────────────
    // public List<float[]> embedBatch(List<String> texts) {
    // if (!enabled || apiKey.isBlank() || texts.isEmpty()) {
    // return texts.stream().map(t -> new float[0]).toList();
    // }

    // try {
    // Map<String, Object> requestBody = Map.of(
    // "texts", texts,
    // "model", model,
    // "input_type", "search_document" // ← "document" for words being indexed
    // );

    // Map response = webClient.post()
    // .uri("/embed")
    // .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
    // .bodyValue(requestBody)
    // .retrieve()
    // .bodyToMono(Map.class)
    // .timeout(Duration.ofSeconds(30))
    // .block();

    // if (response == null) return List.of();

    // // Cohere returns: { "embeddings": [[...], [...], ...] }
    // List<List<Double>> embeddings =
    // (List<List<Double>>) response.get("embeddings");

    // if (embeddings == null) return List.of();

    // return embeddings.stream()
    // .map(this::toFloatArray)
    // .toList();

    // } catch (Exception e) {
    // log.warn("Batch embedding failed: {}", e.getMessage());
    // return List.of();
    // }
    // }

    // // ── Helpers ───────────────────────────────────────────────────
    // private float[] extractEmbedding(Map response, int index) {
    // if (response == null) return new float[0];

    // // Cohere response: { "embeddings": [[0.1, 0.2, ...]] }
    // List<List<Double>> embeddings =
    // (List<List<Double>>) response.get("embeddings");

    // if (embeddings == null || embeddings.size() <= index) return new float[0];

    // return toFloatArray(embeddings.get(index));
    // }

    // private float[] toFloatArray(List<Double> doubles) {
    // float[] result = new float[doubles.size()];
    // for (int i = 0; i < doubles.size(); i++) {
    // result[i] = doubles.get(i).floatValue();
    // }
    // return result;
    // }

    // ── Refactored Batch embed ───────────────────────────────────────────
    public List<float[]> embedBatch(List<String> texts) {
        if (!enabled || apiKey.isBlank() || texts.isEmpty()) {
            return texts.stream().map(t -> new float[0]).toList();
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "texts", texts,
                    "model", model,
                    "input_type", "search_document");

            Map response = webClient.post()
                    .uri("/embed")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null)
                return List.of();

            // Fix 1: Cast to wildcard Number instead of Double to tolerate raw
            // Integers/Longs
            List<List<? extends Number>> embeddings = (List<List<? extends Number>>) response.get("embeddings");

            if (embeddings == null)
                return List.of();

            return embeddings.stream()
                    .map(this::toFloatArray)
                    .toList();

        } catch (Exception e) {
            log.error("Batch embedding failed", e); // Use log.error and pass the whole exception to see stack trace if
                                                    // it persists
            return List.of();
        }
    }

    // ── Refactored Helpers ───────────────────────────────────────────
    private float[] extractEmbedding(Map response, int index) {
        if (response == null)
            return new float[0];

        List<List<? extends Number>> embeddings = (List<List<? extends Number>>) response.get("embeddings");

        if (embeddings == null || embeddings.size() <= index)
            return new float[0];

        return toFloatArray(embeddings.get(index));
    }

    private float[] toFloatArray(List<? extends Number> numbers) {
        float[] result = new float[numbers.size()];
        for (int i = 0; i < numbers.size(); i++) {
            // Fix 2: Calling .floatValue() on the base Number class handles both Integers
            // and Doubles safely
            if (numbers.get(i) != null) {
                result[i] = numbers.get(i).floatValue();
            }
        }
        return result;
    }
}