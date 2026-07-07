package com.project.auto_complete_service.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LlmService {

    private final WebClient webClient;

    @Value("${llm.groq.api-key}")
    private String apiKey;

    @Value("${llm.groq.model:llama3-8b-8192}")
    private String model;

    @Value("${llm.groq.max-tokens:100}")
    private int maxTokens;

    @Value("${llm.groq.enabled:true}")
    private boolean enabled;

    public LlmService(@Value("${llm.groq.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ── Main method: expand a query into suggestions ──────────────
    public List<String> expandQuery(String query, int limit) {
        if (!enabled) {
            log.debug("LLM disabled — skipping expansion for '{}'", query);
            return List.of();
        }

        try {
            String prompt = buildPrompt(query, limit);
            String response = callGroq(prompt);
            List<String> suggestions = parseResponse(response, query);

            log.info("LLM expanded '{}' → {}", query, suggestions);
            return suggestions;

        } catch (Exception e) {
            // LLM failure must NEVER break the search response
            log.warn("LLM expansion failed for '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    // ── Build the prompt ──────────────────────────────────────────
    private String buildPrompt(String query, int limit) {
        return String.format("""
                You are a search autocomplete engine.
                Given a partial search query, suggest %d relevant completions.
                
                Rules:
                - Return ONLY a JSON array of strings
                - Each string is a complete search term
                - Completions must start with or be closely related to the input
                - Keep each suggestion under 5 words
                - No explanations, no numbering, just the JSON array
                
                Partial query: "%s"
                
                Response (JSON array only):
                """, limit, query);
    }

    // ── Call Groq API ─────────────────────────────────────────────
    private String callGroq(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "temperature", 0.3,      // low temp = consistent results
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        Map response = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(java.time.Duration.ofSeconds(5))  // never wait more than 5s
                .block();

        if (response == null) return "[]";

        // Extract content from response
        List<Map> choices = (List<Map>) response.get("choices");
        if (choices == null || choices.isEmpty()) return "[]";

        Map message = (Map) choices.get(0).get("message");
        return (String) message.get("content");
    }

    // ── Parse LLM response into List<String> ─────────────────────
    private List<String> parseResponse(String content, String originalQuery) {
        if (content == null || content.isBlank()) return List.of();

        try {
            // Extract JSON array from response
            // LLM sometimes adds extra text — find the [...] part
            Pattern pattern = Pattern.compile("\\[.*?\\]", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(content);

            if (!matcher.find()) return List.of();

            String jsonArray = matcher.group();

            // Simple JSON array parse — remove brackets, split by comma
            jsonArray = jsonArray.replace("[", "").replace("]", "");
            String[] parts = jsonArray.split(",");

            List<String> results = new ArrayList<>();
            for (String part : parts) {
                String clean = part.trim()
                        .replace("\"", "")
                        .replace("'", "")
                        .toLowerCase()
                        .trim();

                // Validate: not empty, not too long, related to query
                if (!clean.isBlank()
                        && clean.length() <= 50
                        && clean.length() >= 2) {
                    results.add(clean);
                }
            }

            return results;

        } catch (Exception e) {
            log.warn("Failed to parse LLM response: {}", content);
            return List.of();
        }
    }
}